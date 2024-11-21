package com.example.photo_ia;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.provider.MediaStore;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PICK_IMAGE = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private ImageView imageView;
    private Uri photoURI;
    private File currentPhotoFile;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        imageView = findViewById(R.id.view_image);
        Button btnTakePhoto = findViewById(R.id.btn_take_photo);
        Button btnUploadPhoto = findViewById(R.id.btn_load_photo); // Botón para subir imagen

        // Manejo de la foto tomada
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verificar si el permiso de cámara está concedido
                if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                } else {
                    takePicture();  // Permiso concedido, tomar foto
                }
            }
        });

        // Manejo de la foto subida desde la galería
        btnUploadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(); // Abrir la galería
            }
        });

        // Verificar si el permiso de ubicación está concedido
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Tomar una foto
    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Crear un archivo para guardar la foto
            File photoFile = null;
            try {
                photoFile = createImageFile(); // Crea el archivo para la foto
            } catch (IOException ex) {
                Log.e("HomeActivity", "Error al crear el archivo de la foto", ex);
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(HomeActivity.this,
                        "com.example.photo_ia.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // Crear archivo para guardar la imagen tomada
    private File createImageFile() throws IOException {
        // Crear un nombre único para la foto
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Crear un archivo temporal en el directorio de imágenes
        File image = File.createTempFile(
                imageFileName,  /* prefijo */
                ".jpg",         /* sufijo */
                storageDir      /* directorio */
        );

        // Guardar el URI del archivo para su posterior uso
        currentPhotoFile = image;
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                // Verificar que el archivo de la foto no sea nulo
                if (currentPhotoFile != null && currentPhotoFile.exists()) {
                    // Decodificar la imagen desde la ruta del archivo
                    Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoFile.getAbsolutePath());

                    if (imageBitmap != null) {
                        // Mostrar la imagen en el ImageView
                        imageView.setImageBitmap(imageBitmap);

                        // Convertir el Bitmap a ByteArray
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();

                        // Enviar la imagen como ByteArray al siguiente Activity
                        Intent intent = new Intent(HomeActivity.this, AnimalRecognitionActivity.class);
                        intent.putExtra("image_bitmap", byteArray);
                        startActivity(intent); // Iniciar el siguiente Activity
                    } else {
                        Log.e("ERROR", "No se pudo cargar la imagen.");
                    }
                } else {
                    Log.e("ERROR", "El archivo de la foto no es válido.");
                }
            } catch (Exception e) {
                Log.e("ERROR", "Excepción al cargar la imagen: " + e.getMessage());
            }

        } else if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();

            // Verificar si la URI seleccionada es válida
            if (selectedImage != null) {
                imageView.setImageURI(selectedImage); // Muestra la imagen seleccionada

                // Convertir la imagen seleccionada en ByteArray
                Bitmap selectedBitmap = null;
                try {
                    selectedBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                if (selectedBitmap != null) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();

                    // Enviar la imagen seleccionada como ByteArray al siguiente Activity
                    Intent intent = new Intent(HomeActivity.this, AnimalRecognitionActivity.class);
                    intent.putExtra("image_bitmap", byteArray);
                    startActivity(intent); // Iniciar el siguiente Activity
                }
            } else {
                Log.e("ERROR", "La URI seleccionada no es válida.");
            }
        }
    }

    // Método para abrir la galería
    private void openGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, PICK_IMAGE);
    }

    // Manejar la respuesta de los permisos solicitados
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture(); // Permiso concedido, tomar foto
                } else {
                    Log.e("HomeActivity", "Permiso de cámara denegado");
                }
                break;
        }
    }
}
