package com.example.photo_ia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

public class AnimalRecognitionActivity extends AppCompatActivity {

    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animalrecognition);

        resultTextView = findViewById(R.id.result_text);

        // Obtener el ByteArray de la imagen pasada a través del Intent
        byte[] byteArray = getIntent().getByteArrayExtra("image_bitmap");
        if (byteArray != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            classifyImage(bitmap); // Clasificar la imagen
        } else {
            resultTextView.setText("Error: No se pasó ninguna imagen.");
        }
    }

    private void classifyImage(Bitmap bitmap) {
        if (bitmap == null) {
            resultTextView.setText("Error: La imagen es nula.");
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // Crear opciones para el etiquetador de imágenes
        ImageLabelerOptions options =
                new ImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.5f) // Ajusta el umbral de confianza según sea necesario
                        .build();

        // Obtener el cliente con las opciones especificadas
        ImageLabeler labeler = ImageLabeling.getClient(options);

        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    if (labels.isEmpty()) {
                        resultTextView.setText("No se encontraron etiquetas.");
                    } else {
                        StringBuilder result = new StringBuilder();
                        for (ImageLabel label : labels) {
                            Log.d("ImageLabel", "Etiqueta: " + label.getText() + ", Confianza: " + label.getConfidence());
                            result.append(label.getText()).append(" (Confianza: ").append(label.getConfidence()).append(")\n");
                        }
                        resultTextView.setText(result.toString()); // Mostrar los resultados en la UI
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ImageClassification", "Error al clasificar la imagen", e);
                    resultTextView.setText("Error: " + e.getMessage());
                });
    }
}
