package dev.notalpha.dashloader.mixin.main;

import dev.notalpha.dashloader.cache.CacheManager;
import dev.notalpha.dashloader.client.DashToast;
import dev.notalpha.dashloader.minecraft.DashLoaderClientDriver;
import dev.notalpha.dashloader.util.ProfilerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceReload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = SplashOverlay.class, priority = 69420)
public class SplashScreenMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	private long reloadCompleteTime;

	@Shadow
	@Final
	private ResourceReload reload;

	@Mutable
	@Shadow
	@Final
	private boolean reloading;

	@Inject(
			method = "render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J", shift = At.Shift.BEFORE, ordinal = 1)
	)
	private void done(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		this.client.setOverlay(null);
		if (this.client.currentScreen != null) {
			if (this.client.currentScreen instanceof TitleScreen) {
				this.client.currentScreen = new TitleScreen(false);
			}
		}

		ProfilerUtil.getTimeStringFromStart(ProfilerUtil.RELOAD_START);
		if (DashLoaderClientDriver.MANAGER.getStatus() == CacheManager.Status.SAVE && client.getToastManager().getToast(DashToast.class, Toast.TYPE) == null) {
			client.getToastManager().add(new DashToast(DashLoaderClientDriver.MANAGER));
		} else {
			DashLoaderClientDriver.MANAGER.setStatus(CacheManager.Status.IDLE);
		}
	}

	@Inject(
			method = "render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourceReload;isComplete()Z", shift = At.Shift.BEFORE)
	)
	private void removeMinimumTime(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (this.reloadCompleteTime == -1L && this.reload.isComplete()) {
			this.reloading = false;
		}
	}
}