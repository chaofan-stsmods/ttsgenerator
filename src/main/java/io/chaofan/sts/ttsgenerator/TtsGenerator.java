package io.chaofan.sts.ttsgenerator;

import basemod.BaseMod;
import basemod.interfaces.PostRenderSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import io.chaofan.sts.ttsgenerator.cards.GoldenTicket;
import io.chaofan.sts.ttsgenerator.model.CardSetDef;
import io.chaofan.sts.ttsgenerator.model.TabletopCardDef;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@SpireInitializer
public class TtsGenerator implements PostRenderSubscriber {
    private static final int yCut = 30;

    private boolean saved = false;

    public static boolean isGenerating = false;
    private static String generatingFileName;
    public static Map<String, TabletopCardDef> cardMap = new HashMap<>();
    private static Texture cardGlowAttack;
    private static Texture cardGlowSkill;
    private static Texture cardGlowPower;

    public static void initialize() {
        BaseMod.subscribe(new TtsGenerator());
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        if (saved) {
            return;
        }

        saved = true;

        cardGlowAttack = ImageMaster.loadImage("ttsgenerator/images/glowattack.png");
        cardGlowSkill = ImageMaster.loadImage("ttsgenerator/images/glowskill.png");
        cardGlowPower = ImageMaster.loadImage("ttsgenerator/images/glowpower.png");

        // Load card definition
        // See TabletopCardDef.java for whole definition
        loadCardDefinition("ironclad");

        // Generate deck
        generateCardSet(sb, "ironcladbasic");

        System.out.println("Generator Done");

        System.exit(0);
    }

    private void loadCardDefinition(String name) {
        Gson gson = new Gson();
        Type cardDefMapType = (new TypeToken<Map<String, TabletopCardDef>>() {}).getType();
        String cards = Gdx.files.internal("ttsgenerator/cards/"+ name + ".json").readString();
        cardMap.putAll(gson.fromJson(cards, cardDefMapType));
    }

    private void generateCardSet(SpriteBatch sb, String name) {
        try {
            isGenerating = true;
            Gson gson = new Gson();
            String cardSet = Gdx.files.internal("ttsgenerator/cardsets/" + name + ".json").readString();
            CardSetDef csd = gson.fromJson(cardSet, CardSetDef.class);
            generateCards(sb, csd, name);
        } finally {
            isGenerating = false;
        }
    }

    private void generateCards(SpriteBatch sb, CardSetDef csd, String filename) {
        generatingFileName = filename;
        SingleCardViewPopup scv = new SingleCardViewPopup();

        int bw = 2112;
        int bh = 1188;
        int w = 744;
        int h = 1039;
        int pw = 744 * csd.width;
        int ph = 1039 * csd.height;

        float factor = Math.max((float)pw / Settings.WIDTH, (float)ph / Settings.HEIGHT);

        FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, bw, bh, false, false);
        TextureRegion textureRegion = new TextureRegion(fb.getColorBufferTexture(), (bw - w) / 2, (bh - h) / 2 + yCut, w, h - yCut);
        FrameBuffer panel = new FrameBuffer(Pixmap.Format.RGB888, (int) (Settings.WIDTH * factor) + 1, (int) (Settings.HEIGHT * factor) + 1, false, false);

        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sb.end();

        renderBuffer(sb, csd, scv, w, h, pw, ph, factor, fb, textureRegion, panel, false);
        renderBuffer(sb, csd, scv, w, h, pw, ph, factor, fb, textureRegion, panel, true);

        panel.end();

        sb.begin();

        fb.dispose();
        panel.dispose();
    }

    private void renderBuffer(SpriteBatch sb, CardSetDef csd, SingleCardViewPopup scv, int w, int h, int pw, int ph, float factor, FrameBuffer fb, TextureRegion textureRegion, FrameBuffer panel, boolean upgraded) {
        panel.begin();
        if (upgraded) {
            Gdx.gl.glClearColor(csd.upgradeColorR / 255f, csd.upgradeColorG / 255f, csd.upgradeColorB / 255f, 1.0F);
        } else {
            Gdx.gl.glClearColor(csd.colorR / 255f, csd.colorG / 255f, csd.colorB / 255f, 1.0F);
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        panel.end();

        SingleCardViewPopup.enableUpgradeToggle = false;

        AbstractCard.CardColor lastCardColor = null;

        outloop:
        for (int y = 0, c = 0; y < csd.height; y++) {
            for (int x = 0; x < csd.width; x++, c++) {
                if (c >= csd.list.size()) {
                    break outloop;
                }

                String cardId = csd.list.get(c);
                AbstractCard card;
                if (cardId.equals("ttsgen:GoldenTicket")) {
                    card = new GoldenTicket(lastCardColor);
                } else {
                    card = CardLibrary.getCard(cardId).makeCopy();
                    lastCardColor = card.color;
                }
                SingleCardViewPopup.isViewingUpgrade = upgraded;
                scv.open(card);
                CardCrawlGame.isPopupOpen = false;

                fb.begin();
                Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                sb.begin();
                scv.render(sb);
                sb.end();
                fb.end();

                panel.begin();
                sb.begin();
                sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                TabletopCardDef cardDef = cardMap.get(card.cardID);
                Texture cardGlow = cardGlowSkill;
                if ((cardDef != null && "ATTACK".equals(cardDef.type)) ||
                        ((cardDef == null || cardDef.type == null) && card.type == AbstractCard.CardType.ATTACK)) {
                    cardGlow = cardGlowAttack;
                } else if ((cardDef != null && "POWER".equals(cardDef.type)) ||
                        ((cardDef == null || cardDef.type == null) && card.type == AbstractCard.CardType.POWER)) {
                    cardGlow = cardGlowPower;
                }

                float scale = 1f / factor;
                if (upgraded) {
                    sb.setColor(csd.upgradeGlowR / 255f, csd.upgradeGlowG / 255f, csd.upgradeGlowB / 255f, 1.0F);
                } else {
                    sb.setColor(csd.glowR / 255f, csd.glowG / 255f, csd.glowB / 255f, 0.7F);
                }
                sb.draw(cardGlow, x * w * scale, (y + 1) * h * scale, w * scale, -h * scale);
                sb.setColor(Color.WHITE);
                sb.draw(textureRegion, x * w * scale, y * h * scale, w * scale, (h - yCut) * scale);
                sb.end();
                panel.end();
            }
        }

        SingleCardViewPopup.enableUpgradeToggle = true;

        panel.begin();
        Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, pw, ph);
        PixmapIO.writePNG(new FileHandle(!upgraded ? generatingFileName + ".png" : generatingFileName + "_upgraded.png"), pixmap);
    }
}
