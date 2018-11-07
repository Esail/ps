import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import context.Context;
import data.*;
import evaluate.AUC;
import evaluate.LossSurface;
import evaluate.SoftmaxPrecision;
import loss.CrossEntropy;
import model.DNN;
import model.FullConnectedNN;
import model.Model;
import net.PServer;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jblas.FloatMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import train.Trainer;
import update.AdamUpdater;
import update.FtrlUpdater;
import update.Updater;
import util.MatrixUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class Mnist extends DataSet {

	private static Logger logger = LoggerFactory.getLogger(CTR.class);

	static DataSet trainSet;

	static DataSet testSet;

	public Mnist(Parser parser, DataSource source, int batch, int thread) {
		super(parser, source, batch, thread);
	}

	public static class MnistParser implements Parser {
		@Override
		public List<Feature> parse(String line) {
			List<Feature> result = Lists.newArrayList();
			String[] cols = line.split(",");
			for (int i=0; i<cols.length; i++) {
				result.add(new Feature(i, Float.parseFloat(cols[i])));
			}
			return result;
		}
	}

	@Override
	public Map<String, FloatMatrix> parseFeature(List<List<Feature>> dataList) {
		int N = dataList.size();
		float[][] X = new float[784][N];
		float[][] Y = new float[1][N];
		for (int i=0; i<dataList.size(); i++) {
			List<Feature> cols = dataList.get(i);
			Y[0][i] = cols.get(0).toF();
			for (int j = 1; j < cols.size(); j++) {
				X[j-1][i] = cols.get(j).toF();
			}
		}
		Map<String, FloatMatrix> result = Maps.newHashMap();
		result.put("X", new FloatMatrix(X));
		result.put("Y", new FloatMatrix(Y));
		return result;
	}

	public static void main(String args[]) throws Exception {
		Context.init();

		Context.thread = 4;
		//Context.workerNum = 2;

		Context.mode = Context.Mode.DISTRIBUTED;

		//Context.isPsAsync = true;
		//Context.isPs = true;

		if (Context.isPServer()) {
			// 启动PS进程
			Updater updater = new AdamUpdater(0.005, 0.9, 0.999, Math.pow(10, -8));

			Updater ftrl = new FtrlUpdater(0.005f, 1f, 0.001f, 0.001f);

			PServer server = new PServer(Context.psPort, Context.workerNum);

			server.getUpdaterMap().put(updater.getName(), updater);

			server.getUpdaterMap().put(ftrl.getName(), ftrl);

			server.start();

			System.exit(0);
		}

		trainSet = new Mnist(new MnistParser(), new FileSource(new File(System.getProperty("train",
				Mnist.class.getResource("").getPath()+"../../src/main/resources/mnist_train.csv"))), 1000, 1);

		testSet = new Mnist(new MnistParser(), new FileSource(new File(System.getProperty("test",
				Mnist.class.getResource("").getPath()+"../../src/main/resources/mnist_test.csv"))), 1000, 1);

		Trainer trainer = new Trainer(Context.thread, new Callable<Model>() {

			@Override
			public Model call() throws Exception {
				return FullConnectedNN.buildModel(784, new int[]{150, 50, 10});
			}

		});

		for (int epoch = 0; epoch < 20 && !Context.finish; epoch++) {
			logger.info("epoch {}", epoch);
			train(trainer);
			precision(trainer);
		}
	}

	private static void train(Trainer trainer) throws IOException {
		while (trainSet.hasNext()) {

			List<Map<String, FloatMatrix>> dataList = Lists.newArrayList();

			for (int i=0; i<Context.thread; i++) {

				Map<String, FloatMatrix> datas = trainSet.next();

				if (datas == null || datas.isEmpty()) {

					continue;

				}

				dataList.add(datas);

			}

			trainer.train(dataList);
		}

		trainSet.reset();
	}

	private static void precision(Trainer trainer) throws IOException {
		logger.info("begin compute precision...");
		List<Float[]> p = Lists.newArrayList();
		List<Float> l = Lists.newArrayList();
		while (testSet.hasNext()) {
			List<Map<String, FloatMatrix>> dataList = Lists.newArrayList();
			List<FloatMatrix> y = Lists.newArrayList();
			for (int i=0; i<Context.thread; i++) {
				Map<String, FloatMatrix> datas = testSet.next();
				if (datas == null || datas.isEmpty()) {
					continue;
				}
				y.add(datas.get("Y"));
				dataList.add(datas);
			}
			FloatMatrix[] ps = trainer.predict(dataList);
			for (int i=0; i<y.size(); i++) {
				if (ps[i] == null) {
					continue;
				}
				for (int j=0; j<y.get(i).length; j++) {
					l.add(y.get(i).data[j]);
					Float[] c = new Float[ps[i].rows];
					for (int k=0; k<ps[i].rows; k++) {
						c[k] = ps[i].data[ps[i].index(k, j)];
					}
					p.add(c);
				}
			}
		}
		SoftmaxPrecision precision = new SoftmaxPrecision(l, p);
		logger.info("Precision {}", precision.calculate());
		testSet.reset();
	}


	private static void auc(Trainer trainer) throws IOException {
		logger.info("begin compute auc...");
		List<Pair<Double, Double>> data = new ArrayList<Pair<Double, Double>>();
		while (testSet.hasNext()) {
			List<Map<String, FloatMatrix>> dataList = Lists.newArrayList();
			List<FloatMatrix> y = Lists.newArrayList();
			for (int i=0; i<Context.thread; i++) {
				Map<String, FloatMatrix> datas = testSet.next();
				if (datas == null || datas.isEmpty()) {
					// logger.info("read data eof");
					break;
				}
				y.add(datas.get("Y"));
				dataList.add(datas);
			}
			FloatMatrix[] ps = trainer.predict(dataList);
			for (int i=0; i<y.size(); i++) {
				if (ps[i] == null) {
					continue;
				}
				for (int j=0; j<y.get(i).length; j++) {
					data.add(new MutablePair<Double, Double>((double) ps[i].data[j], (double) y.get(i).data[j]));
				}
			}
		}
		AUC auc = new AUC(data);
		logger.info("AUC {}", auc.calculate());
		testSet.reset();
	}
}
