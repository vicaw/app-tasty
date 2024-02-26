package com.vicaw.receitasfinal.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.vicaw.receitasfinal.R;
import com.vicaw.receitasfinal.activity.MainActivity;
import com.vicaw.receitasfinal.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private TextInputLayout layoutEmail;
    private TextInputEditText inputEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText inputPassword;

    private LinearLayout  linkSingUpLayout;
    private ProgressBar progressBar;
    private Button buttonLogin;
    private TextView linkSingUp;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        layoutEmail = binding.loginLayoutEmail;
        inputEmail = binding.loginInputEmail;
        layoutPassword = binding.loginLayoutPassword;
        inputPassword = binding.loginInputPassword;
        buttonLogin = binding.loginButtonLogin;
        linkSingUp = binding.loginLinkSingUp;
        progressBar = binding.loginProgressBar;
        linkSingUpLayout = binding.loginLinkSingUpLayout;

        if(isAuthenticated())
            goToMainActivity();

        buttonLogin.setOnClickListener(view -> {
            if (isFormValid()) {
                login(inputEmail.getText().toString().trim(), inputPassword.getText().toString().trim());
            }
        });

        linkSingUp.setOnClickListener(view -> {
            Navigation.findNavController(root).navigate(R.id.action_loginFragment_to_registerFragment);
        });


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private Boolean isAuthenticated(){
       return firebaseAuth.getCurrentUser() != null;
    }

    private Boolean isFormValid() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        layoutEmail.setError(null);
        layoutPassword.setError(null);

        if (email.isEmpty()) {
            layoutEmail.setError("Informe seu e-mail");
            return false;
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("E-mail inválido");
            return false;
        }
        else if (password.isEmpty()) {
            layoutPassword.setError("Informe sua senha");
            return false;
        }
        else if (password.length() < 6) {
            layoutPassword.setError("A senha deve ter entre 6 e 20 caracteres");
            return false;
        }

        return true;
    }

    private void login(String email, String password){
        progressBar.setVisibility(View.VISIBLE);
        buttonLogin.setVisibility(View.GONE);
        linkSingUpLayout.setVisibility(View.GONE);



        Task<AuthResult> loginResult = firebaseAuth.signInWithEmailAndPassword(email, password);
        loginResult.addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            buttonLogin.setVisibility(View.VISIBLE);
            linkSingUpLayout.setVisibility(View.VISIBLE);

            if(task.isSuccessful()) {
                goToMainActivity();
            } else {
                try {
                    throw task.getException();
                } catch (FirebaseAuthException e) {
                    String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                    String message = "Erro ao tentar autenticar.";
                    switch (errorCode){
                        case "ERROR_INVALID_EMAIL":
                            message = "Verifique o e-mail informado.";
                            break;

                        case "ERROR_USER_NOT_FOUND":
                        case "ERROR_WRONG_PASSWORD":
                            message = "E-mail ou senha inválidos.";
                            break;

                    }
                    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                    Log.d("LOGIN EXCEPTION", errorCode);
                } catch (Exception e) {
                    Log.d("LOGIN EXCEPTION", e.getMessage());
                }

            }
        });

    }

    private void goToMainActivity(){
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


}