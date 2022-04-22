package com.sammy.malum.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.sammy.malum.common.blockentity.totem.TotemPoleTileEntity;
import com.sammy.malum.core.handlers.RenderHandler;
import com.sammy.malum.core.setup.content.SpiritTypeRegistry;
import com.sammy.malum.core.helper.RenderHelper;
import com.sammy.malum.core.systems.rendering.RenderTypes;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.HashMap;

import static com.sammy.malum.core.helper.RenderHelper.FULL_BRIGHT;


public class TotemPoleRenderer implements BlockEntityRenderer<TotemPoleTileEntity> {
    public static HashMap<MalumSpiritType, Material> overlayHashmap = new HashMap<>();

    public TotemPoleRenderer(BlockEntityRendererProvider.Context context) {
        SpiritTypeRegistry.SPIRITS.forEach((s, t) ->
                overlayHashmap.put(t, new Material(TextureAtlas.LOCATION_BLOCKS, t.getOverlayTexture()))
        );
    }

    @Override
    public void render(TotemPoleTileEntity blockEntityIn, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        Direction direction = blockEntityIn.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (blockEntityIn.type == null) {
            return;
        }
        renderQuad(overlayHashmap.get(blockEntityIn.type), blockEntityIn.type.color, blockEntityIn.currentColor/20f, direction, poseStack);
    }

    public void renderQuad(Material material, Color color, float alpha, Direction direction, PoseStack poseStack) {
        TextureAtlasSprite sprite = material.sprite();
        VertexConsumer consumer = RenderHandler.DELAYED_RENDER.getBuffer(RenderTypes.ADDITIVE_BLOCK);

        Vector3f[] positions = new Vector3f[]{new Vector3f(0, 0, 2.01f), new Vector3f(2, 0, 2.01f), new Vector3f(2, 2, 2.01f), new Vector3f(0, 2, 2.01f)};

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Vector3f.YN.rotationDegrees(direction.toYRot()));
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        RenderHelper.create()
                .setColor(color, alpha)
                .setLight(FULL_BRIGHT)
                .setUV(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1())
                .renderQuad(consumer, poseStack, positions, 0.5f);
        poseStack.popPose();
    }

    public float rotation(Direction direction) {
        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            direction = direction.getOpposite();
        }
        return direction.toYRot();
    }
}