package com.badlogic.gdx.graphics.g2d;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class SpriteBatchAlt extends SpriteBatch {
    @Override
    public void flush() {
        if (this.idx != 0) {
            ++this.renderCalls;
            ++this.totalRenderCalls;
            int spritesInBatch = this.idx / 20;
            if (spritesInBatch > this.maxSpritesInBatch) {
                this.maxSpritesInBatch = spritesInBatch;
            }

            int count = spritesInBatch * 6;
            this.lastTexture.bind();
            Mesh mesh = ReflectionHacks.getPrivate(this, SpriteBatch.class, "mesh");
            mesh.setVertices(this.vertices, 0, this.idx);
            mesh.getIndicesBuffer().position(0);
            mesh.getIndicesBuffer().limit(count);
            if (ReflectionHacks.getPrivate(this, SpriteBatch.class, "blendingDisabled")) {
                Gdx.gl.glDisable(3042);
            } else {
                Gdx.gl.glEnable(3042);
                Gdx.gl.glBlendFuncSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }

            ShaderProgram customShader = ReflectionHacks.getPrivate(this, SpriteBatch.class, "customShader");
            ShaderProgram shader = ReflectionHacks.getPrivate(this, SpriteBatch.class, "shader");
            mesh.render(customShader != null ? customShader : shader, 4, 0, count);
            this.idx = 0;
        }
    }
}
