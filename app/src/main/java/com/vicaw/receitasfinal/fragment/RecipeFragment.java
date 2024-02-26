package com.vicaw.receitasfinal.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.vicaw.receitasfinal.R;
import com.vicaw.receitasfinal.adapter.IngredientListAdapter;
import com.vicaw.receitasfinal.databinding.FragmentRecipeBinding;
import com.vicaw.receitasfinal.model.Recipe;

import java.util.ArrayList;
import java.util.Arrays;


public class RecipeFragment extends Fragment {
    private FragmentRecipeBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference recipesRef;
    private DatabaseReference recipesRatingRef;
    private DatabaseReference usersRef;
    private DatabaseReference favoritesRef;
    private StorageReference storageRef;
    private ProgressBar imageProgressBar;
    private ImageView recipeImage;
    private TextView recipeName;
    private TextView recipeAuthor;
    private TextView recipeScore;
    private RatingBar ratingBarIndicator;
    private TextView ratingCount;
    private ListView ingredientsList;
    private TextView recipeSteps;
    private RatingBar ratingBar;
    private Button shareButton;
    private Button favoriteButton;
    private LinearLayout contentLayout;
    private ProgressBar progressBar;
    private Recipe recipe;

    private ValueEventListener valueEventListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getArguments();
        String recipeId = bundle.getString("recipeId", "");
        recipe = new Recipe();
        recipe.setId(recipeId);

        firebaseAuth = FirebaseAuth.getInstance();

        recipesRef = FirebaseDatabase.getInstance().getReference()
                .child("recipes")
                .child(recipe.getId());

        recipesRatingRef = recipesRef.child("ratedBy");

        usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        storageRef = FirebaseStorage.getInstance().getReference();

        favoritesRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(firebaseAuth.getCurrentUser().getUid())
                .child("favorites")
                .child(recipe.getId());

    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRecipeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        imageProgressBar = binding.recipeImageProgressBar;
        recipeImage = binding.recipeRecipeImage;
        recipeName = binding.recipeTitle;
        recipeAuthor = binding.recipeAuthor;
        recipeScore = binding.recipeScore;
        ratingBarIndicator = binding.recipeRatingBarIndicator;
        ratingCount = binding.recipeRatingCount;
        ingredientsList = binding.recipeIngredientsList;
        recipeSteps = binding.recipeSteps;
        ratingBar = binding.recipeRatingBar;

        favoriteButton = binding.recipeFavoriteButton;
        shareButton = binding.recipeShareButton;

        progressBar = binding.recipeProgressBar;
        contentLayout = binding.recipeContentLayout;


        favoriteButton.setOnClickListener(view -> {
            favoriteToggle();
        });

        shareButton.setOnClickListener(view -> {
            share();
        });

        ratingBar.setOnRatingBarChangeListener((ratingBar1, rating, b) -> {
            setUserRating(rating);
        });

        getRecipeFromDB();
        setFavoriteButton();
        setRecipeRating();


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        recipesRatingRef.removeEventListener(valueEventListener);
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

    private void setUserRating(float rating){
        recipesRatingRef.child(firebaseAuth.getUid()).setValue(rating);
    }

    private void setRecipeRating() {
        //ValueEventListener separado porque é necessário remove-lo no onDestroyView()
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double avgRating = 0.0;

                for (DataSnapshot ratingData: snapshot.getChildren()) {
                    String userId = ratingData.getKey();
                    Double rating = ratingData.getValue(Double.class);
                    avgRating += rating;

                    if (userId.equals(firebaseAuth.getCurrentUser().getUid())){
                        ratingBar.setRating(rating.floatValue());
                    }

                }

                long childrenCount = snapshot.getChildrenCount();
                if(childrenCount > 0) {
                    avgRating = avgRating / (double) childrenCount;
                }

                ratingCount.setText(getString(R.string.recipe_ratingCount, Long.toString(childrenCount)));
                ratingBarIndicator.setRating(avgRating.floatValue());
                recipeScore.setText(String.format("%.1f", avgRating));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };



        recipesRatingRef.addValueEventListener(valueEventListener);
    }

    private void setImage(String imageName){
        StorageReference image = storageRef.child("recipes/" + imageName);
        Glide.with(getContext())
                .load(image)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        imageProgressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(recipeImage);
    }


    private void setFavoriteButton() {
        favoritesRef.get().addOnSuccessListener(dataSnapshot -> {
            if(dataSnapshot.getValue() != null)
                ((MaterialButton) favoriteButton).setIcon(AppCompatResources.getDrawable(getContext(), R.drawable.baseline_favorite_24));
        });
    }

    private void getRecipeFromDB(){
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        recipesRef.get().addOnSuccessListener(dataSnapshot -> {
            recipe = dataSnapshot.getValue(Recipe.class);

            setImage(recipe.getImageName());
            recipeName.setText(recipe.getName());
            recipeSteps.setText(recipe.getSteps());

            //Como os ingredientes estão armazenados como uma String única,
            //para exibi-los no listView é necessário criar uma lista,
            //separando os ingredientes pela quebra de linha.
            ArrayList<String> ingredients = new ArrayList<String>(Arrays.asList(recipe.getIngredients().split("\n")));
            ingredientsList.setAdapter(new IngredientListAdapter(getContext(), ingredients));
            setListViewHeightBasedOnChildren(ingredientsList);

            //Pede o nome do autor
            usersRef.child(recipe.getPostedBy()).child("name").get()
                    .addOnSuccessListener(data -> {
                        String authorName = data.getValue(String.class);
                        recipeAuthor.setText(getString(R.string.recipe_author, authorName));

                        progressBar.setVisibility(View.GONE);
                        contentLayout.setVisibility(View.VISIBLE);
                    });

        });

    }

    private void favoriteToggle(){
        favoritesRef.get().addOnSuccessListener(dataSnapshot -> {
            if(dataSnapshot.getValue() == null){
                favoritesRef.setValue(true);
                ((MaterialButton) favoriteButton).setIcon(AppCompatResources.getDrawable(getContext(), R.drawable.baseline_favorite_24));
                Snackbar.make(getView(), "Receita adicionada aos favoritos.", Snackbar.LENGTH_LONG).show();
            }
            else{
                favoritesRef.removeValue();
                ((MaterialButton) favoriteButton).setIcon(AppCompatResources.getDrawable(getContext(), R.drawable.outline_favorite_border_24));
                Snackbar.make(getView(), "Receita removida dos favoritos.", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void share() {
        String name = recipe.getName();
        String ingredients = recipe.getIngredients();
        String steps = recipe.getSteps();

        String shareMessage = "\n"+ name +"\n\n";
        shareMessage += ingredients + "\n\n";
        shareMessage += steps;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Receita de" + name);
        intent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(intent, "Escolha onde compartilhar"));
    }


    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight() + 5;
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();

    }
}

