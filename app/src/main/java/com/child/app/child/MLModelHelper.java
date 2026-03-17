package com.child.app.child;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MLModelHelper {
    private Interpreter interpreter;

    public MLModelHelper(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws Exception {
        AssetFileDescriptor fileDescriptor =
                context.getAssets().openFd("malware_model.tflite");
        FileInputStream inputStream =
                new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
        );
    }

    // ML Prediction
    public float predict(float[] input) {
        if (interpreter == null) return 0f;
        float[][] output = new float[1][1];
        float[][] inputArray = new float[1][input.length];
        inputArray[0] = input;
        interpreter.run(inputArray, output);
        return output[0][0];
    }
}
