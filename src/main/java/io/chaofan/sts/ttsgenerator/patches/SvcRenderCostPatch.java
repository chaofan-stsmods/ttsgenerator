package io.chaofan.sts.ttsgenerator.patches;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

@SpirePatch(clz = SingleCardViewPopup.class, method = "renderCost")
public class SvcRenderCostPatch {
    private static final float costScale = 0.9f;
    private static final float costOffset = 15f;

    @SpireInstrumentPatch
    public static ExprEditor zoomCostIcon() {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("renderHelper")) {
                    m.replace(String.format("{ %s.zoomCostIcon($$); }", SvcRenderCostPatch.class.getName()));
                }
            }
        };
    }

    public static void zoomCostIcon(SpriteBatch sb, float x, float y, TextureAtlas.AtlasRegion img) {
        x += costOffset * Settings.scale;
        y -= costOffset * Settings.scale;
        if (img != null) {
            sb.draw(
                img,
                x + img.offsetX - (float)img.originalWidth / 2.0F,
                y + img.offsetY - (float)img.originalHeight / 2.0F,
                (float)img.originalWidth / 2.0F - img.offsetX,
                (float)img.originalHeight / 2.0F - img.offsetY,
                (float)img.packedWidth, (float)img.packedHeight,
                Settings.scale * costScale,
                Settings.scale * costScale,
                0.0F);
        }
    }

    @SpireInstrumentPatch
    public static ExprEditor moveCostNumber() {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("renderFont")) {
                    m.replace(String.format("{ %s.moveCostNumber($$); }", SvcRenderCostPatch.class.getName()));
                }
            }
        };
    }

    public static void moveCostNumber(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        x += costOffset * Settings.scale;
        y -= costOffset * Settings.scale;
        font.getData().setScale(costScale);
        FontHelper.renderFont(sb, font, msg, x, y, c);
        font.getData().setScale(1);
    }
}
