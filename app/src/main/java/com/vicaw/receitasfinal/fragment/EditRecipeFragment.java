package com.vicaw.receitasfinal.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
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
import java.util.HashMap;
import java.util.UUID;

public class EditRecipeFragment extends Fragment {

    private FragmentNewRecipeBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference recipeRef;
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
    private Recipe recipe;

    private Bitmap originalImage;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getArguments();
        String recipeId = bundle.getString("recipeId", "");
        recipe = new Recipe();
        recipe.setId(recipeId);

        firebaseAuth = FirebaseAuth.getInstance();
        recipeRef = FirebaseDatabase.getInstance().getReference()
                .child("recipes")
                .child(recipe.getId());
        firebaseAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        getRecipeFromDB();

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

        buttonFinish.setText(R.string.editRecipe_save);

        buttonPickImage.setOnClickListener(view -> {
            selectImage();
        });

        buttonFinish.setOnClickListener(view -> {
            if(isFormValid()) {
                recipe.setPostedBy(firebaseAuth.getCurrentUser().getUid());
                recipe.setName(inputName.getText().toString());
                recipe.setIngredients(inputIngredients.getText().toString());
                recipe.setSteps(inputSteps.getText().toString());

                updateRecipe(recipe);
            }
        });

        //getRecipeFromDB();

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
            }
        });

    }

    private void selectImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activityResultLauncher.launch(intent);
    }

    private void getRecipeFromDB(){
        recipeRef.get().addOnSuccessListener(dataSnapshot -> {
            recipe = dataSnapshot.getValue(Recipe.class);
            StorageReference imageRef = storageRef.child("recipes/" + recipe.getImageName());
            Glide.with(requireContext())
                    .load(imageRef)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                           // buttonPickImage.setImageResource(R.drawable.baseline_image_24);
                            return false;
                        }
                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            buttonPickImage.setImageTintList(null);
                            originalImage = ((BitmapDrawable)resource).getBitmap();
                            return false;
                        }
                    })
                    .into(buttonPickImage);

            inputName.setText(recipe.getName());
            inputIngredients.setText(recipe.getIngredients());
            inputSteps.setText(recipe.getSteps());
        });
    }
    public void updateRecipe(Recipe recipe) {
        progressBar.setVisibility(View.VISIBLE);
        buttonFinish.setVisibility(View.GONE);

        Bitmap bitmap = ((BitmapDrawable) buttonPickImage.getDrawable()).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

        //Como o firebase deleta todas as entradas que não foram referenciadas no update,
        //é necessário adicionar o score atual junto.
        DatabaseReference scoreRef = recipeRef.child("ratedBy");

        scoreRef.get().addOnSuccessListener(dataSnapshot -> {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", recipe.getName());
            hashMap.put("ingredients", recipe.getIngredients());
            hashMap.put("postedBy", recipe.getPostedBy());
            hashMap.put("steps", recipe.getSteps());
            hashMap.put("ratedBy", dataSnapshot.getValue());

            //Se a imagem não for a original
            if (!bitmap.sameAs(originalImage)) {
                Log.d("EDIT RECIPE", "IMAGE CHANGED");
                byte[] imageData = byteArrayOutputStream.toByteArray();

                StorageReference originalImageRef = storageRef.child("recipes/" + recipe.getImageName());
                String imageId = firebaseAuth.getCurrentUser().getUid() + "_" + UUID.randomUUID().toString();
                StorageReference imageRef = storageRef.child("recipes/" + imageId + ".png");
                UploadTask uploadTask = imageRef.putBytes(imageData);

                uploadTask.addOnSuccessListener(taskSnapshot -> {
                            hashMap.put("imageName", imageId + ".png");
                            recipeRef.updateChildren(hashMap)
                                    .addOnSuccessListener(Void -> {
                                        //Deleta a imagem original
                                        originalImageRef.delete();
                                        Snackbar.make(getView(), "Receita modificada com sucesso.", Snackbar.LENGTH_LONG).show();
                                        Navigation.findNavController(getView()).navigate(R.id.action_navigation_editRecipe_to_navigation_myRecipes);
                                    })
                                    .addOnFailureListener(e -> {
                                        imageRef.delete();
                                        Snackbar.make(getView(), "Falha ao modificar a receita.", Snackbar.LENGTH_LONG).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Snackbar.make(getView(), "Falha ao fazer o upload da imagem.", Snackbar.LENGTH_LONG).show();
                        });

            } else {
                Log.d("EDIT RECIPE", "IMAGE UNCHANGED");
                hashMap.put("imageName", recipe.getImageName());
                recipeRef.updateChildren(hashMap).addOnSuccessListener(unused -> {
                    Snackbar.make(getView(), "Receita modificada com sucesso.", Snackbar.LENGTH_LONG).show();
                    Navigation.findNavController(getView()).navigate(R.id.action_navigation_editRecipe_to_navigation_myRecipes);
                });
            }
        });





    }



}