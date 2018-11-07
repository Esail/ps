package net;

public class Mod implements Router {
	private final int n;
	Mod(int n) {
		this.n = n;
	}
	@Override
	public int shard(String key) {
		return key.hashCode() % n;
	}
}
