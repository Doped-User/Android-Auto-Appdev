package com.example.HardwareInfo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText editPassword;
    private TextInputLayout passwordLayout;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        editPassword = findViewById(R.id.etPassword);
        passwordLayout = findViewById(R.id.etPasswordLayout);
        Button btnSave = findViewById(R.id.btnSave);

        passwordLayout.setEndIconDrawable(R.drawable.ic_visibility_off);

        passwordLayout.setEndIconOnClickListener(v -> {
            if (isPasswordVisible) {
                editPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordLayout.setEndIconDrawable(R.drawable.ic_visibility_off);
            } else {
                editPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                passwordLayout.setEndIconDrawable(R.drawable.ic_visibility_on);
            }
            isPasswordVisible = !isPasswordVisible;
            editPassword.setSelection(editPassword.length());
        });

        // Save button click listener
        btnSave.setOnClickListener(v -> {
            String newPassword = editPassword.getText().toString().trim();

            if (!newPassword.isEmpty()) {
                SharedPreferences sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("password", newPassword);
                editor.apply();
                Toast.makeText(ChangePasswordActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(ChangePasswordActivity.this, "Please enter a new password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
