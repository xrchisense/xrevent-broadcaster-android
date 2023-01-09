/**
 * For references please look up the Khronos OpenGL ES Reference Pages at
 * https://registry.khronos.org/OpenGL-Refpages/
 * resprectively the Android wrapper API at
 * https://developer.android.com/reference/android/opengl/GLES30
 */

package com.xrchisense.xrevent.broadcasterplugin;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

public class FBOUtils {

    // Compile type dependent shader
    public static int compileShader(int type, String shaderCode) {
        // Specifies the type of shader to be created. Returns ID.
        final int shaderObjectId = GLES30.glCreateShader(type);
        if (shaderObjectId == 0) {
            return 0;
        }
        // Sets the source code in shader object to the source code provided as string.
        GLES30.glShaderSource(shaderObjectId, shaderCode);
        // Compile shader.
        GLES30.glCompileShader(shaderObjectId);
        // To check compile result.
        final int[] compileStatus = new int[1];
        // glGetShaderiv: More general function used to verify the result in both shader stage and OpenGL program stage。
        GLES30.glGetShaderiv(shaderObjectId, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            log("compileShader error --> " + shaderCode);
            // Delete on failure.
            GLES30.glDeleteShader(shaderObjectId);
            return 0;
        }
        return shaderObjectId;
    }

    // Create OpenGL and link.
    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        // Create OpenGL Program and check return ID for error.
        final int programObjectId = GLES30.glCreateProgram();
        if (programObjectId == 0) {
            return 0;
        }
        // Link vertex shader.
        GLES30.glAttachShader(programObjectId, vertexShaderId);
        // Link fragment shader.
        GLES30.glAttachShader(programObjectId, fragmentShaderId);
        // After linking the shaders, link the OpenGL program.
        GLES30.glLinkProgram(programObjectId);
        // Verify link result for error.
        final int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(programObjectId, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            log("linkProgram error");
            // Delete OpenGL program if failure occurs.
            GLES30.glDeleteProgram(programObjectId);
            return 0;
        }
        return programObjectId;
    }

    // After linking the OpenGL program，Check whether OpenGL is available。
    public static boolean validateProgram(int programObjectId) {
        GLES30.glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        GLES30.glGetProgramiv(programObjectId, GLES30.GL_VALIDATE_STATUS, validateStatus, 0);
        return validateStatus[0] != 0;
    }

    // Create OpenGL program.
    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
        // Compile the vertex shader.
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource);
        // Compile the fragment shader.
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource);
        int program = linkProgram(vertexShader, fragmentShader);
        boolean valid = validateProgram(program);
        log("buildProgram valid = " + valid);
        return program;
    }

    public static int createFBO() {
        int[] fbo = new int[1];
        GLES30.glGenFramebuffers(fbo.length, fbo, 0);
        return fbo[0];
    }

    public static int createVAO() {
        int[] vao = new int[1];
        GLES30.glGenVertexArrays(vao.length, vao, 0);
        return vao[0];
    }

    public static int createVBO() {
        int[] vbo = new int[1];
        GLES30.glGenBuffers(2, vbo, 0);
        return vbo[0];
    }

    public static int createOESTextureID() {
        int[] texture = new int[1];
        GLES30.glGenTextures(texture.length, texture, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        //GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glGenerateMipmap(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        return texture[0];
    }

    public static int create2DTextureId(int width, int height) {
        int[] textures = new int[1];
        GLES30.glGenTextures(textures.length, textures, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        return textures[0];
    }

    public static void log(String msg) {
        Log.i("PVRFbo", msg);
    }

    public static void checkError(String msg) {
        log(msg + " -- error --> " + GLES30.glGetError());
    }

}
