package com.vicaw.receitasfinal.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.vicaw.receitasfinal.databinding.ActivityLoginRegisterBinding;

public class LoginRegisterActivity extends AppCompatActivity {
    private ActivityLoginRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }


}