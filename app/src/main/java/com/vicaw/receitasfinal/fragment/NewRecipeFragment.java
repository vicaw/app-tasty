package com.vicaw.receitasfinal.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vicaw.receitasfinal.R;
import com.vicaw.receitasfinal.databinding.FragmentNewRecipeBinding;
import com.vicaw.receitasfinal.model.Recipe;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class NewRecipeFragment extends Fragment {

    private FragmentNewRecipeBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference recipesRef;
    private StorageReference storageRef;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private ImageButton buttonPickImage;
    private TextInputLayout layoutName;
    private TextInputEditText inputName;
    private TextInputLayout layoutIngredients;
    private TextInputEditText inputIngredients;
    private TextInputLayout layoutSteps;
    private TextInputEditText inputSteps;
    private Button buttonFinish;
    private ProgressBar progressBar;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        recipesRef = FirebaseDatabase.getInstance().getReference().child("recipes");
        storageRef = FirebaseStorage.getInstance().getReference();

        activityResultLauncherInit();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNewRecipeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        buttonPickImage = binding.newRecipeButtonPickImage;
        layoutName = binding.newRecipeLayoutName;
        inputName = binding.newRecipeInputName;
        layoutIngredients = binding.newRecipeLayoutIngredients;
        inputIngredients = binding.newRecipeInputIngredients;
        layoutSteps = binding.newRecipeLayoutSteps;
        inputSteps = binding.newRecipeInputSteps;
        buttonFinish = binding.newRecipeButtonFinish;
        progressBar = binding.newRecipeProgressBar;

        buttonPickImage.setOnClickListener(view -> {
            selectImage();
        });

        buttonFinish.setOnClickListener(view -> {
            if(isFormValid()) {
                Recipe recipe = new Recipe();

                recipe.setPostedBy(firebaseAuth.getCurrentUser().getUid());
                recipe.setName(inputName.getText().toString());
                recipe.setIngredients(inputIngredients.getText().toString());
                recipe.setSteps(inputSteps.getText().toString());

                addRecipeToDB(recipe);
            }
        });


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        toggleBottomNavigationBar(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        toggleBottomNavigationBar(View.GONE);
    }

    private void toggleBottomNavigationBar(int state) {
        getActivity().findViewById(R.id.nav_view).setVisibility(state);
    }

    private Boolean isFormValid() {
        String name = inputName.getText().toString().trim();
        String ingredients = inputIngredients.getText().toString().trim();
        String steps = inputSteps.getText().toString().trim();

        layoutName.setError(null);
        layoutIngredients.setError(null);
        layoutSteps.setError(null);

        if (name.isEmpty()) {
            layoutName.setError("Informe o nome da receita");
            return false;
        }
        else if(ingredients.isEmpty()) {
            layoutIngredients.setError("Informe os ingredientes da receita");
            return false;
        }
        else if(steps.isEmpty()) {
            layoutSteps.setError("Informe o modo de preparo da receita");
            return false;
        }
        else if( !(buttonPickImage.getDrawable() instanceof BitmapDrawable)){
            Snackbar.make(getView(), "Escolha uma imagem para a receita.", Snackbar.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private void activityResultLauncherInit() {
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getData() != null) {
                Uri imageUri = result.getData().getData();
                buttonPickImage.setImageURI(imageUri);
                buttonPickImage.setImageTintList(null);
            }
        });

    }

    private void selectImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activityResultLauncher.launch(intent);
    }

    public void addRecipeToDB(Recipe recipe) {
        progressBar.setVisibility(View.VISIBLE);
        buttonFinish.setVisibility(View.GONE);

        //Gera um nome único para receita, baseado no id do usuario + string aleatória.
        String imageId = firebaseAuth.getCurrentUser().getUid() + "_" + UUID.randomUUID().toString();

        //Prepara a imagem da receita
        Bitmap bitmap = ((BitmapDrawable) buttonPickImage.getDrawable()).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] imageData = byteArrayOutputStream.toByteArray();

        //Faz upload da receita
        StorageReference imageRef = storageRef.child("recipes/" + imageId + ".png");
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
                    //Se o upload for feito com sucesso, adiciona o nome da imagem ao objeto
                    recipe.setImageName(imageId + ".png");

                    //E adiciona ao banco
                    recipesRef.push().setValue(recipe)
                            .addOnSuccessListener(Void -> {
                                Snackbar.make(getView(), "Receita cadastrada com sucesso.", Snackbar.LENGTH_LONG).show();
                                Navigation.findNavController(getView()).navigate(R.id.action_navigation_newRecipe_to_navigation_myRecipes);
                            })
                            .addOnFailureListener(e -> {
                                //Deleta a imagem caso tenha alguma falha no cadastro
                                imageRef.delete();
                                Snackbar.make(getView(), "Falha ao cadastrar a receita.", Snackbar.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(getView(), "Falha ao fazer o upload da imagem.", Snackbar.LENGTH_LONG).show();
                });
    }


    //imageRef.getDownloadUrl().addOnSuccessListener(onSuccessListener)
}