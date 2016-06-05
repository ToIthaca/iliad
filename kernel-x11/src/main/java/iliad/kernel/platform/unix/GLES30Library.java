package iliad.kernel.platform.unix;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;

import java.nio.*;

public interface GLES30Library extends Library {

    GLES30Library INSTANCE = (GLES30Library) Native.loadLibrary("libGLESv2", GLES30Library.class);

    void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1,
                           int destX0, int destY0, int destX1, int destY1, int bitMask, int filter);
    void glViewport(int x, int y, int width, int height);
    void glFlush();
    void glClear(int bitMask);
    void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data);
    void glClearColor(float red, float green, float blue, float alpha);
    void glEnable(int cap);
    void glDisable(int cap);
    int glGetError();
    int glCreateShader(int type);
    void glShaderSource(int shader, int count, StringArray sources, IntBuffer lengths);
    void glDeleteShader(int sid);
    void glCompileShader(int sid);
    void glAttachShader(int pid, int sid);
    void glGetShaderiv(int sid, int name, IntBuffer ptr);
    void glGetShaderInfoLog(int shader, int maxLogLen, IntBuffer length, ByteBuffer logData);
    int glCreateProgram();
    void glUseProgram(int pid);
    void glLinkProgram(int pid);
    void glGetProgramiv(int pid, int name, IntBuffer ptr);
    void glGenBuffers(int num, IntBuffer ptr);
    void glBindBuffer(int target, int bid);
    void glBufferData(int target, int size, Buffer data, int usage);
    void glBufferSubData(int target, int offset, int size, Buffer data);
    void glEnableVertexAttribArray(int location);
    void glVertexAttribPointer(int location, int size, int type, boolean normalized, int stride, int offset);
    void glGenFramebuffers(int num, IntBuffer ptr);
    void glBindFramebuffer(int target, int fid);
    void glFramebufferRenderbuffer(int target,int attachment,int rTarget,int rid);
    int glCheckFramebufferStatus(int target);
    void glFramebufferTexture2D(int target, int attachment, int texTarget, int texture, int level);
    void glGenRenderbuffers(int num, IntBuffer ptr);
    void glBindRenderbuffer(int target, int rid);
    void glRenderbufferStorage(int target,int format,int width,int height);
    void glBindTexture(int target, int tid);
    void glGenTextures(int num, IntBuffer ptr);
    void glTexParameteri(int target, int name, int value);
    void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer data);
    void glPixelStorei(int name, int value);
    void glActiveTexture(int texture);
    void glDrawArrays(int mode, int first, int count);
    void glDrawElements(int mode, int count, int type, int offset);
    void glUniform1i(int location, int arg0);
    void glUniform1f(int location, float arg0);
    void glUniform1fv(int location, int count, FloatBuffer ptr);
    void glUniform1iv(int location, int count, IntBuffer ptr);
    void glUniform2i(int location, int arg0, int arg1);
    void glUniform2f(int location, float arg0, float arg1);
    void glUniform2fv(int location, int count, FloatBuffer ptr);
    void glUniform2iv(int location, int count, IntBuffer ptr);
    void glUniform3i(int location, int arg0, int arg1, int arg2);
    void glUniform3f(int location, float arg0, float arg1, float arg2);
    void glUniform3fv(int location, int count, FloatBuffer ptr);
    void glUniform3iv(int location, int count, IntBuffer ptr);
    void glUniform4i(int location, int arg0, int arg1, int arg2, int arg3);
    void glUniform4f(int location, float arg0, float arg1, float arg2, float arg3);
    void glUniform4fv(int location, int count, FloatBuffer ptr);
    void glUniform4iv(int location, int count, IntBuffer ptr);
    void glUniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer arg0);
    void glUniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer arg0);
    void glUniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer arg0);
    int glGetAttribLocation(int pid, String name);
    int glGetUniformLocation(int pid, String name);
    void glGetIntegerv(int name, IntBuffer ptr);

    //GLES30Library
    void glGenSamplers(int num, IntBuffer buffer);
    void  glSamplerParameteri(int sid, int name, int arg0);
    void glBindSampler(int tid, int sid);
    void glCopyBufferSubData(int readTarget, int writeTarget,int readOffset,int writeOffset, int size);
    void glBindVertexArray(int vid);
    void glDrawBuffers(int num, IntBuffer buffers);
    void glClearBufferuiv(int target, int drawBuffer, IntBuffer value);
    void glClearBufferfv(int target, int drawBuffer, FloatBuffer value);
    void glReadBuffer(int num);
    void glDrawElementsInstanced(int mode, int count, int type, int offset, int primCount);

    void glBindAttribLocation(int program,int index,String name);
    void glBlendColor(float red,float green,float blue, float alpha);
    void glBlendEquation(int mode);
    void glBlendEquationSeparate(int modeRGB,int modeAlpha);
    void glBlendFunc(int sfactor, int dfactor);
    void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha);
    void glClearDepthf(float d);
    void glClearStencil(int s);
    void glColorMask(boolean red,boolean green, boolean blue, boolean alpha);
    void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, Buffer data);
    void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, Buffer data);
    void glCopyTexImage2D(int target,int level, int internalformat,int x, int y, int width, int height, int border);
    void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset,int x,int y,int width,int height);
    void glCullFace(int mode);
    void glDeleteBuffers(int n, int[] buffers);
    void glDeleteFramebuffers(int n, int[] framebuffers);
    void glDeleteProgram(int program);
    void glDeleteRenderbuffers(int n, int[] renderbuffers);
    void glDeleteTextures(int n, int[] textures);
    void glDepthFunc(int func);
    void glDepthMask(boolean flag);
    void glDepthRangef(float n,float f);
    void glDetachShader(int program, int shader);
    void glDisableVertexAttribArray(int index);
    void glDrawElements(int mode,int count,int type, Buffer indices);
    void glFinish();
    void glFrontFace(int mode);
    void glGenerateMipmap(int target);
    void glGetActiveAttrib(int program,int index,int bufSize, IntBuffer length, IntBuffer size, IntBuffer type, Buffer name);
    void glGetActiveUniform(int program,int index,int bufSize,IntBuffer length,IntBuffer size,IntBuffer type, Buffer name);
    void glGetAttachedShaders(int program,int maxCount,IntBuffer count,IntBuffer shaders);
    void glGetBooleanv(int pname,IntBuffer data);
    void glGetBufferParameteriv(int target,int pname, IntBuffer params);
    void glGetFloatv(int pname, FloatBuffer data);
    void glGetFramebufferAttachmentParameteriv(int target,int attachment,int pname, IntBuffer params);
    void glGetProgramInfoLog(int program,int bufSize,IntBuffer length, Buffer infoLog);
    void glGetRenderbufferParameteriv(int target,int pname,IntBuffer params);
    void glGetShaderInfoLog(int shader,int bufSize,IntBuffer length,Buffer infoLog);
    void glGetShaderPrecisionFormat(int shadertype,int precisiontype,IntBuffer range,IntBuffer  precision);
    void glGetShaderSource(int shader,int bufSize,IntBuffer length, Buffer source);
    String glGetString(int name);
    void glGetTexParameterfv(int target,int pname,FloatBuffer params);
    void glGetTexParameteriv(int target,int pname,IntBuffer params);
    void glGetUniformfv(int program,int location,FloatBuffer params);
    void glGetUniformiv(int program,int location,IntBuffer params);
    void glGetVertexAttribPointerv(int index,int pname,Buffer[] pointer);
    void glGetVertexAttribfv(int index,int pname,FloatBuffer params);
    void glGetVertexAttribiv(int index,int pname,IntBuffer params);
    void glHint(int target,int mode);
    boolean glIsBuffer(int buffer);
    boolean glIsEnabled(int cap);
    boolean glIsFramebuffer(int framebuffer);
    boolean glIsProgram(int program);
    boolean glIsRenderbuffer(int renderbuffer);
    boolean glIsShader(int shader);
    boolean glIsTexture(int texture);
    void glLineWidth(float width);
    void glPolygonOffset(float factor,float units);
    void glReadPixels(int x,int y,int width,int height,int format,int type, Buffer pixels);
    void glReleaseShaderCompiler();
    void glSampleCoverage(float value,boolean invert);
    void glScissor(int x,int y,int width,int height);
    void glShaderBinary(int count,int[] shaders,int binaryformat, Buffer binary, int length);
    void glShaderSource(int shader,int count,String[] string, int[] length);
    void glStencilFunc(int func,int ref,int mask);
    void glStencilFuncSeparate(int face,int func,int ref,int mask);
    void glStencilMask(int mask);
    void glStencilMaskSeparate(int face,int mask);
    void glStencilOp(int fail,int zfail,int zpass);
    void glStencilOpSeparate(int face,int sfail,int dpfail,int dppass);
    void glTexParameterf(int target,int pname,float param);
    void glTexParameterfv(int target,int pname,float[] params);
    void glTexParameteriv(int target,int pname,int[] params);
    void glTexSubImage2D(int target,int level,int xoffset,int yoffset,int width,int height,int format,int type, Buffer pixels);
    void glUniform1fv(int location,int count,float[]value);
    void glUniform1iv(int location,int count,int[] value);
    void glUniform2fv(int location,int count,float[]value);
    void glUniform2iv(int location,int count,int[]value);
    void glUniform3fv(int location,int count,float[]value);
    void glUniform3iv(int location,int count,int[]value);
    void glUniform4fv(int location,int count,float[]value);
    void glUniform4iv(int location,int count,int[]value);
    void glUniformMatrix2fv(int location,int count,boolean transpose,float[] value);
    void glUniformMatrix3fv(int location,int count,boolean transpose,float[] value);
    void glUniformMatrix4fv(int location,int count,boolean transpose,float[] value);
    void glValidateProgram(int program);
    void glVertexAttrib1f (int index,float x);
    void glVertexAttrib1fv(int index,float[] v);
    void glVertexAttrib2f (int index,float x,float y);
    void glVertexAttrib2fv(int index,float[] v);
    void glVertexAttrib3f (int index,float x, float y,float z);
    void glVertexAttrib3fv(int index,float[] v);
    void glVertexAttrib4f (int index,float x,float y, float z, float w);
    void glVertexAttrib4fv(int index,float[] v);
    void glVertexAttribPointer(int index,int size,int type,boolean normalized,int stride, Buffer pointer);
    void glVertexAttribIPointer(int index,int size,int type,int stride, int offset);

    void glBeginQuery(int target,int id);
    void glBeginTransformFeedback(int primitiveMode);
    void glBindBufferBase(int target,int index,int buffer);
    void glBindBufferRange(int target,int index,int buffer,int offset,int size);
    void glBindTransformFeedback(int target,int id);
    void glClearBufferfi(int buffer,int drawbuffer,float depth,int stencil);
    void glClearBufferfv(int buffer,int drawbuffer,float[] value);
    void glClearBufferiv(int buffer,int drawbuffer,int[] value);
    void glClearBufferuiv(int buffer,int drawbuffer,int[] value);
    int glClientWaitSync(long sync,int flags,long timeout);
    void glCompressedTexImage3D(int target,int level,int internalformat,int width,int height,int depth,int border,int imageSize,Buffer data);
    void glCompressedTexSubImage3D(int target,int level,int xoffset,int yoffset,int zoffset,int width,int height,int depth,int format,int imageSize,Buffer data);
    void glCopyTexSubImage3D(int target,int level,int xoffset,int yoffset,int zoffset,int x,int y,int width,int height);
    void glDeleteQueries(int n,int[] ids);
    void glDeleteSamplers(int count,int[] samplers);
    void glDeleteSync(long sync);
    void glDeleteTransformFeedbacks(int n,int[] ids);
    void glDeleteVertexArrays(int n,int[] arrays);
    void glDrawArraysInstanced(int mode,int first,int count,int instancecount);
    void glDrawElementsInstanced(int mode,int count,int type,Buffer indices,int instancecount);
    void glDrawRangeElements(int mode,int start,int end,int count,int type,int offset);
    void glDrawRangeElements(int mode,int start,int end,int count,int type,Buffer indices);
    void glEndQuery(int target);
    void glEndTransformFeedback();
    long glFenceSync(int condition,int flags);
    void glFlushMappedBufferRange(int target,int offset,int length);
    void glFramebufferTextureLayer(int target,int attachment,int texture,int level,int layer);
    void glGenQueries(int n,IntBuffer ids);
    void glGenTransformFeedbacks(int n,IntBuffer ids);
    void glGenVertexArrays(int n,IntBuffer arrays);
    void glGetActiveUniformBlockName(int program,int uniformBlockIndex,int bufSize,IntBuffer legnth,Buffer uniformBlockName);
    void glGetActiveUniformBlockiv(int program,int uniformBlockIndex,int pname,IntBuffer params);
    void glGetActiveUniformsiv(int program,int uniformCount,int[] uniformIndices,int pname,IntBuffer params);
    void glGetBufferParameteri64v(int target,int pname,LongBuffer params);
    void glGetBufferPointerv(int target,int pname,Buffer[] params);
    int glGetFragDataLocation(int program,String name);
    void glGetInteger64i_v(int target,int index,LongBuffer data);
    void glGetInteger64v(int pname,LongBuffer data);
    void glGetIntegeri_v(int target,int index,IntBuffer data);
    void glGetInternalformativ(int target,int internalformat,int pname,int bufSize,IntBuffer params);
    void glGetProgramBinary(int program,int bufSize,IntBuffer legnth, IntBuffer binaryFormat, Buffer binary);
    void glGetQueryObjectuiv(int id,int pname,IntBuffer params);
    void glGetQueryiv(int target,int pname,IntBuffer params);
    void glGetSamplerParameterfv(int sampler,int pname, FloatBuffer params);
    void glGetSamplerParameteriv(int sampler,int pname,IntBuffer params);
    String glGetStringi(int name,int index);
    void glGetSynciv(long sync,int pname,int bufSize,IntBuffer legnth,IntBuffer values);
    void glGetTransformFeedbackVarying(int program,int index,int bufSize,IntBuffer legnth,IntBuffer size,IntBuffer type,Buffer name);
    int glGetUniformBlockIndex(int program,String uniformBlockName);
    void glGetUniformIndices(int program,int uniformCount, String[] uniformNames,IntBuffer uniformIndices);
    void glGetUniformuiv(int program,int location,IntBuffer params);
    void glGetVertexAttribIiv(int index,int pname,IntBuffer params);
    void glGetVertexAttribIuiv(int index,int pname,IntBuffer params);
    void glInvalidateFramebuffer(int target,int numAttachments,IntBuffer attachments);
    void glInvalidateSubFramebuffer(int target,int numAttachments,IntBuffer attachments,int x,int y,int width,int height);
    boolean glIsQuery(int id);
    boolean glIsSampler(int sampler);
    boolean glIsSync(long sync);
    boolean glIsTransformFeedback(int id);
    boolean glIsVertexArray(int array);
    Buffer glMapBufferRange(int target,int offset,int length,int access);
    void glPauseTransformFeedback();
    void glProgramBinary(int program,int binaryFormat,Buffer binary,int length);
    void glProgramParameteri(int program,int pname,int value);
    void glRenderbufferStorageMultisample(int target,int samples,int internalformat,int width,int height);
    void glResumeTransformFeedback();
    void glSamplerParameterf(int sampler,int pname,float param);
    void glSamplerParameterfv(int sampler,int pname,float[] param);
    void glSamplerParameteriv(int sampler,int pname,int[] param);
    void glTexImage3D(int target,int level,int internalformat,int width,int height,int depth,int border,int format,int type,Buffer pixels);
    void glTexStorage2D(int target,int levels,int internalformat,int width,int height);
    void glTexStorage3D(int target,int levels,int internalformat,int width,int height,int depth);
    void glTexSubImage3D(int target,int level,int xoffset,int yoffset,int zoffset,int width,int height,int depth,int format,int type,Buffer pixels);
    void glTransformFeedbackVaryings(int program,int count,String[] varyings,int bufferMode);
    void glUniform1ui(int location,int v0);
    void glUniform1uiv(int location,int count,int[] value);

    void glUniform2ui(int location,int v0,int v1);
    void glUniform2uiv(int location,int count,int[] value);
    void glUniform3ui(int location,int v0,int v1,int v2);
    void glUniform3uiv(int location,int count,int[] value);
    void glUniform4ui(int location,int v0,int v1,int v2,int v3);
    void glUniform4uiv(int location,int count,int[] value);
    void glUniformBlockBinding(int program,int uniformBlockIndex,int uniformBlockBinding);
    void glUniformMatrix2x3fv(int location,int count,boolean transpose,float[] value);
    void glUniformMatrix2x4fv(int location,int count,boolean transpose,float[] value);
    void glUniformMatrix3x2fv(int location,int count,boolean transpose,float[] value);
    void glUniformMatrix3x4fv(int location,int count,boolean transpose,float[] value);
    void glUniformMatrix4x2fv(int location,int count,boolean transpose,float[] value);
    void glUniformMatrix4x3fv(int location,int count,boolean transpose,float[] value);
    boolean glUnmapBuffer(int target);
    void glVertexAttribDivisor(int index,int divisor);
    void glVertexAttribI4i(int index,int x,int y,int z,int w);
    void glVertexAttribI4iv(int index,int[] v);
    void glVertexAttribI4ui(int index,int x,int y,int z,int w);
    void glVertexAttribI4uiv(int index,int[] v);
    void glVertexAttribIPointer(int index,int size,int type,int stride, Buffer pointer);
    void glWaitSync(long sync, int flags, long timeout);
}
