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

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vicaw.receitasfinal.R;
import com.vicaw.receitasfinal.activity.MainActivity;
import com.vicaw.receitasfinal.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;
    private FirebaseAuth firebaseAuth;

    private TextInputLayout layoutEmail;
    private TextInputLayout layoutName;
    private TextInputLayout layoutPassword;
    private TextInputLayout layoutPasswordConfirmation;

    private TextInputEditText inputEmail;
    private TextInputEditText inputName;
    private TextInputEditText inputPassword;
    private TextInputEditText inputPasswordConfirmation;

    private LinearLayout linkLoginLayout;
    private ProgressBar progressBar;

    private Button buttonSignUp;
    private TextView linkLogin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        layoutEmail = binding.registerLayoutEmail;
        layoutName = binding.registerLayoutName;
        layoutPassword = binding.registerLayoutPassword;
        layoutPasswordConfirmation = binding.registerLayoutPasswordConfirmation;

        inputEmail = binding.registerInputEmail;
        inputName = binding.registerInputName;
        inputPassword = binding.registerInputPassword;
        inputPasswordConfirmation = binding.registerInputPasswordConfirmation;

        progressBar = binding.registerProgressBar;
        linkLoginLayout = binding.registerLinkLoginLayout;

        buttonSignUp = binding.registerButtonSingUp;
        linkLogin = binding.registerLinkLogin;


        buttonSignUp.setOnClickListener(view -> {
            if (isFormValid()) {
                register(inputEmail.getText().toString().trim(), inputPassword.getText().toString().trim(), inputName.getText().toString().trim());
            }
        });

        linkLogin.setOnClickListener(view -> {
            Navigation.findNavController(root).navigate(R.id.action_registerFragment_to_loginFragment);
        });


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private Boolean isFormValid() {
        String email = inputEmail.getText().toString().trim();
        String name = inputName.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String passwordConfirmation = inputPasswordConfirmation.getText().toString().trim();

        layoutEmail.setError(null);
        layoutPassword.setError(null);
        layoutPasswordConfirmation.setError(null);
        layoutName.setError(null);

        if (email.isEmpty()) {
            layoutEmail.setError("Informe seu e-mail");
            return false;
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            layoutEmail.setError("E-mail inválido");
            return false;
        }
        else if (name.isEmpty()) {
            layoutName.setError("Informe o seu nome");
            return false;
        }
        else if (name.length() < 3 || name.length() > 70) {
            layoutName.setError("O nome deve ter entre 3 e 70 caracteres");
            return false;
        }
        else if(password.isEmpty()) {
            layoutPassword.setError("Informe sua senha");
            return false;
        }
        else if(password.length() < 6 || password.length() > 20) {
            layoutPassword.setError("A senha deve ter entre 6 e 20 caracteres");
            return false;
        }
        else if(passwordConfirmation.isEmpty()) {
            layoutPasswordConfirmation.setError("Confirme sua senha");
            return false;
        }
        else if(!passwordConfirmation.equals(password)) {
            layoutPasswordConfirmation.setError("As senhas não conferem");
            return false;
        }

        return true;
    }

    private void register(String email, String password, String displayName) {
        progressBar.setVisibility(View.VISIBLE);
        buttonSignUp.setVisibility(View.GONE);
        linkLoginLayout.setVisibility(View.GONE);

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            buttonSignUp.setVisibility(View.VISIBLE);
            linkLoginLayout.setVisibility(View.VISIBLE);

            if(task.isSuccessful()) {
                //Adiciona uma entrada na tabela dos usuários com o id da conta criada
                DatabaseReference users = FirebaseDatabase.getInstance().getReference()
                        .child("users")
                        .child(firebaseAuth.getCurrentUser().getUid());

                //Adiciona o nome nessa tabela
                users.child("name").setValue(displayName);

                goToMainActivity();

                Snackbar.make(getView(), "Conta cadastrada com sucesso.", Snackbar.LENGTH_LONG).show();
            } else {
                try {
                    throw task.getException();
                } catch (FirebaseAuthException e) {
                    String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                    String message = "Erro ao tentar cadastrar.";
                    switch (errorCode) {
                        case "ERROR_INVALID_EMAIL":
                            message = "Verifique o e-mail informado.";
                            break;

                        case "ERROR_EMAIL_ALREADY_IN_USE":
                            message = "O e-mail informado já está cadastrado.";
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