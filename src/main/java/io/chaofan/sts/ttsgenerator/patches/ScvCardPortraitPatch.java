package io.chaofan.sts.ttsgenerator.patches;

import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import io.chaofan.sts.ttsgenerator.TtsGenerator;
import io.chaofan.sts.ttsgenerator.model.TabletopCardDef;

@SpirePatch(clz = SingleCardViewPopup.class, method = "loadPortraitImg")
public class ScvCardPortraitPatch {
    @SpirePostfixPatch
    public static void Postfix(SingleCardViewPopup instance, AbstractCard ___card, @ByRef Texture[] ___portraitImg) {
        TabletopCardDef cardDef = TtsGenerator.cardMap.get(___card.cardID);
        if (cardDef == null) {
            return;
        }

        if (cardDef.image != null) {
            ___portraitImg[0] = ImageMaster.loadImage(cardDef.image);
        }
    }
}
