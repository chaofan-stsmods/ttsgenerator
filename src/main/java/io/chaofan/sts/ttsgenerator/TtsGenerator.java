package io.chaofan.sts.ttsgenerator;

import basemod.BaseMod;
import basemod.interfaces.PostRenderSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatchAlt;
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

    public int outputCounter = 1;

    public static void initialize() {
        BaseMod.subscribe(new TtsGenerator());
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        if (saved) {
            return;
        }

        sb = new SpriteBatchAlt();
        sb.begin();

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
        float factorSingle = Math.max((float)w / Settings.WIDTH, (float)h / Settings.HEIGHT);

        FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, bw, bh, false, false);
        FrameBuffer single = new FrameBuffer(Pixmap.Format.RGBA8888, (int) (Settings.WIDTH * factorSingle) + 1, (int) (Settings.HEIGHT * factorSingle) + 1, false, false);
        TextureRegion textureRegion = new TextureRegion(fb.getColorBufferTexture(), (bw - w) / 2, (bh - h) / 2 + yCut, w, h - yCut);
        FrameBuffer panel = new FrameBuffer(Pixmap.Format.RGBA8888, (int) (Settings.WIDTH * factor) + 1, (int) (Settings.HEIGHT * factor) + 1, false, false);

        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sb.end();

        renderBuffer(sb, csd, scv, w, h, pw, ph, factor, factorSingle, fb, textureRegion, panel, single, false);
        renderBuffer(sb, csd, scv, w, h, pw, ph, factor, factorSingle, fb, textureRegion, panel, single, true);

        sb.begin();

        fb.dispose();
        single.dispose();
        panel.dispose();
    }

    private void renderBuffer(SpriteBatch sb, CardSetDef csd, SingleCardViewPopup scv,
                              int w, int h, int pw, int ph, float factor, float factorSingle,
                              FrameBuffer fb, TextureRegion textureRegion, FrameBuffer panel, FrameBuffer single,
                              boolean upgraded) {
        panel.begin();
        if (upgraded) {
            Gdx.gl.glClearColor(csd.upgradeColorR / 255f, csd.upgradeColorG / 255f, csd.upgradeColorB / 255f, csd.upgradeColorA / 255f);
        } else {
            Gdx.gl.glClearColor(csd.colorR / 255f, csd.colorG / 255f, csd.colorB / 255f, csd.colorA / 255f);
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
                    sb.setColor(csd.upgradeGlowR / 255f, csd.upgradeGlowG / 255f, csd.upgradeGlowB / 255f, csd.upgradeGlowA / 255f);
                } else {
                    sb.setColor(csd.glowR / 255f, csd.glowG / 255f, csd.glowB / 255f, csd.glowA / 255f);
                }
                int drawX = upgraded && csd.flipUpgradedCards ? csd.width - 1 - x : x;
                sb.draw(cardGlow, drawX * w * scale, (y + 1) * h * scale, w * scale, -h * scale);
                sb.setColor(Color.WHITE);
                sb.draw(textureRegion, drawX * w * scale, y * h * scale, w * scale, (h - yCut) * scale);
                sb.end();
                panel.end();

                renderSingleBuffer(sb, csd, w, h, factorSingle, textureRegion, single, upgraded, cardGlow, c);
            }
        }

        SingleCardViewPopup.enableUpgradeToggle = true;

        panel.begin();
        Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, pw, ph);
        PixmapIO.writePNG(new FileHandle("ttsgenerator/" +
                (!upgraded ? generatingFileName + ".png" : generatingFileName + "_upgraded.png")), pixmap);
        panel.end();
    }

    private void renderSingleBuffer(SpriteBatch sb, CardSetDef csd, int w, int h, float factorSingle, TextureRegion textureRegion, FrameBuffer single, boolean upgraded, Texture cardGlow, int c) {
        float singleScale = 1f / factorSingle;
        single.begin();
        sb.begin();
        if (upgraded) {
            Gdx.gl.glClearColor(csd.upgradeColorR / 255f, csd.upgradeColorG / 255f, csd.upgradeColorB / 255f, csd.upgradeColorA / 255f);
        } else {
            Gdx.gl.glClearColor(csd.colorR / 255f, csd.colorG / 255f, csd.colorB / 255f, csd.colorA / 255f);
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (upgraded) {
            sb.setColor(csd.upgradeGlowR / 255f, csd.upgradeGlowG / 255f, csd.upgradeGlowB / 255f, csd.upgradeGlowA / 255f);
        } else {
            sb.setColor(csd.glowR / 255f, csd.glowG / 255f, csd.glowB / 255f, csd.glowA / 255f);
        }
        sb.draw(cardGlow, 0, h * singleScale, w * singleScale, -h * singleScale);
        sb.setColor(Color.WHITE);
        sb.draw(textureRegion, 0, 0, w * singleScale, (h - yCut) * singleScale);
        sb.end();

        Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, w, h);
        PixmapIO.writePNG(new FileHandle("ttsgenerator/" + generatingFileName + "/" +
                (!upgraded ? (c + 1 + csd.singleCardOffset) + ".png" : (c + 1 + csd.singleCardOffset) + "_upgraded.png")), pixmap);

        single.end();
    }
}
