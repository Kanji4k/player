package com.playerspotter.mixin;

import com.playerspotter.SpotterConfig;
import com.playerspotter.SpotterHud;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes other players glow on the client only. This never touches the server,
 * so it works on any server (vanilla, Paper, Spigot, Hypixel, etc.).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

	@Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
	private void playerspotter$forceGlow(CallbackInfoReturnable<Boolean> cir) {
		if (SpotterHud.shouldGlow((Entity) (Object) this)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
	private void playerspotter$glowColor(CallbackInfoReturnable<Integer> cir) {
		if (SpotterHud.shouldGlow((Entity) (Object) this)) {
			cir.setReturnValue(SpotterConfig.INSTANCE.rgb());
		}
	}
}
