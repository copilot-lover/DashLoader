package dev.quantumfusion.dashloader.def.mixin.option.cache.font;

import com.google.common.collect.Lists;
import dev.quantumfusion.dashloader.def.DashLoader;
import dev.quantumfusion.dashloader.def.mixin.accessor.FontManagerAccessor;
import dev.quantumfusion.dashloader.def.mixin.accessor.FontStorageAccessor;
import dev.quantumfusion.dashloader.def.mixin.accessor.UnicodeTextureFontAccessor;
import dev.quantumfusion.dashloader.def.util.mixins.MixinThings;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.font.*;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

@Mixin(targets = "net/minecraft/client/font/FontManager$1")
public class FontManagerOverride {


	@Inject(
			method = {"method_18638", "prepare"},
			at = @At(value = "HEAD"),
			cancellable = true
	)
	private void overridePrepare(ResourceManager resourceManager, Profiler profiler, CallbackInfoReturnable<Map<Identifier, List<Font>>> cir) {
		var fonts = DashLoader.getData().fonts;
		if (fonts.dataAvailable() && DashLoader.isRead()) {
			var cacheResultData = fonts.getCacheResultData();
			cacheResultData.forEach((identifier, list) -> list.forEach(font -> {
						if (font instanceof UnicodeTextureFont) {
							((UnicodeTextureFontAccessor) font).setResourceManager(resourceManager);
						}
					}
			));
			cir.setReturnValue(cacheResultData);
		}
	}


	@Inject(
			method = {"method_18635", "apply"},
			at = @At(value = "HEAD"),
			cancellable = true
	)
	private void overrideApply(Map<Identifier, List<Font>> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
		var fonts = DashLoader.getData().fonts;
		if (fonts.dataAvailable() && DashLoader.isRead()) {
			profiler.startTick();
			profiler.push("closing");
			final FontManagerAccessor fontManagerAccessor = (FontManagerAccessor) MixinThings.FONTMANAGER;
			fontManagerAccessor.getFontStorages().values().forEach(FontStorage::close);
			fontManagerAccessor.getFontStorages().clear();
			profiler.swap("reloading");
			Map<FontStorage, List<Font>> fontMap = new LinkedHashMap<>();
			map.forEach((identifier, fontList) -> {
				FontStorage fontStorage = new FontStorage(fontManagerAccessor.getTextureManager(), identifier);
				prepareFontStorage(((FontStorageAccessor) fontStorage));
				fontManagerAccessor.getFontStorages().put(identifier, fontStorage);
				fontMap.put(fontStorage, fontList);
			});

			fontMap.entrySet().parallelStream().forEach(entry -> computeFontStorages(((FontStorageAccessor) entry.getKey()), Lists.reverse(entry.getValue())));
			profiler.pop();
			profiler.endTick();
			ci.cancel();
		}
	}

	@Inject(method = {"method_18635", "apply"}, at = @At(value = "TAIL"))
	private void applyInject(Map<Identifier, List<Font>> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
		if (DashLoader.isWrite()) {
			DashLoader.getData().fonts.setMinecraftData(map);
		}
	}

	private void prepareFontStorage(FontStorageAccessor access) {
		access.callCloseFonts();
		access.callCloseGlyphAtlases();
		access.getGlyphRendererCache().clear();
		access.getGlyphCache().clear();
		access.getCharactersByWidth().clear();
		access.setBlankGlyphRenderer(access.callGetGlyphRenderer(BlankGlyph.INSTANCE));
		access.setWhiteRectangleGlyphRenderer(access.callGetGlyphRenderer(WhiteRectangleGlyph.INSTANCE));
	}

	private void computeFontStorages(FontStorageAccessor access, List<Font> fonts) {
		final Glyph space = access.getSPACE();

		final IntSet intSet = new IntOpenHashSet();
		final IntFunction<IntList> creatIntArrayListFunc = (i) -> new IntArrayList();
		fonts.forEach(font -> intSet.addAll(font.getProvidedGlyphs()));

		final Set<Font> set = new HashSet<>();
		intSet.forEach((IntConsumer) (codePoint) -> {
			for (Font font : fonts) {
				Glyph glyph = codePoint == 32 ? space : font.getGlyph(codePoint);
				if (glyph != null) {
					set.add(font);
					if (glyph != BlankGlyph.INSTANCE) {
						access.getCharactersByWidth().computeIfAbsent(MathHelper.ceil(glyph.getAdvance(false)), creatIntArrayListFunc).add(codePoint);
					}
					break;
				}
			}

		});
		fonts.stream().filter(set::contains).forEach(access.getFonts()::add);
	}


	@Mixin(FontManager.class)
	private static class LeoFontSolution {
		@Inject(method = "<init>", at = @At(value = "TAIL"))
		private void initInject(TextureManager manager, CallbackInfo ci) {
			MixinThings.FONTMANAGER = ((FontManager) (Object) this);
		}
	}

}