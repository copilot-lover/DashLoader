package dev.notalpha.dashloader.cache.io.data.fragment;

import dev.notalpha.dashloader.cache.io.fragment.Fragment;

public final class ChunkFragment {
	public final FragmentSlice info;

	public ChunkFragment(FragmentSlice info) {
		this.info = info;
	}

	public ChunkFragment(Fragment fragment) {
		this.info = new FragmentSlice(fragment);
	}
}