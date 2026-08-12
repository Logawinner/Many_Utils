package me.anchorhelper.many_utils.mixin;

import me.anchorhelper.many_utils.Metrics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.Mouse")
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, net.minecraft.client.input.MouseInput input, int button, CallbackInfo ci) {
        if (button == 0) {
            Metrics.recordClick(true);
        } else if (button == 1) {
            Metrics.recordClick(false);
        }
    }
}
