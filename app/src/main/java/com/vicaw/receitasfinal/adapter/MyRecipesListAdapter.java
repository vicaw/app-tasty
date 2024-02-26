package com.vicaw.receitasfinal.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.vicaw.receitasfinal.R;
import com.vicaw.receitasfinal.model.Recipe;

import java.util.List;

public class MyRecipesListAdapter extends RecyclerView.Adapter<MyRecipesListAdapter.ViewHolder> {

    private final Context context;
    private List<Recipe> recipes;

    private ItemClickListener mClickListener;

    public MyRecipesListAdapter(Context context, List<Recipe> recipes) {
        this.context = context;
        this.recipes = recipes;
    }

    public MyRecipesListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_my_recipes, parent, false);
        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        DatabaseReference recipesRatingRef = FirebaseDatabase.getInstance().getReference()
                .child("recipes")
                .child(recipe.getId())
                .child("ratedBy");

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference imagens = storageReference.child("recipes/" + recipe.getImageName());

        holder.imageProgressBar.setVisibility(View.VISIBLE);
        holder.recipeImage.setVisibility(View.GONE);

        Glide.with(context)
                .load(imagens).listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.imageProgressBar.setVisibility(View.GONE);
                        holder.recipeImage.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(holder.recipeImage);

        holder.recipeName.setText(recipe.getName());

        //Pode ser substituido por .get()
        recipesRatingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Double avgRating = 0.0;

                for (DataSnapshot ratingData: snapshot.getChildren()) {
                    Double rating = ratingData.getValue(Double.class);
                    avgRating += rating;
                }

                Double childrenCount = Long.valueOf(snapshot.getChildrenCount()).doubleValue();
                avgRating = avgRating / childrenCount;

                holder.recipeRatingBar.setRating(avgRating.floatValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });



        holder.recipeOptions.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(context, holder.recipeOptions);
            popup.inflate(R.menu.popup_menu);
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.popup_excluir:
                        deleteItem(position);
                        return true;
                    case R.id.popup_editar:
                        Bundle bundle = new Bundle();
                        String recipeId = recipe.getId();
                        bundle.putString("recipeId", recipeId);
                        Navigation.findNavController(view).navigate(R.id.navigation_editRecipe, bundle);
                        return true;


                    default:
                        return false;
                }
            });

            popup.show();
        });
    }


    public void setList(List<Recipe> recipes){
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    public void addToList(Recipe recipe){
        this.recipes.add(recipe);
        notifyDataSetChanged();
    }

    public void deleteItem(final int position) {
        new AlertDialog.Builder(context)
                .setTitle("Remover Receita")
                .setMessage("Tem certeza que deseja excluir essa receita?")
                .setPositiveButton("Sim", (dialogInterface, i) -> {
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("recipes");
                    reference.child(getItem(position).getId()).removeValue().addOnSuccessListener(unused -> {
                        Snackbar.make(((Activity)context).findViewById(R.id.container), "Receita Removida.", Snackbar.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Não", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public Recipe getItem(int position) {
        return recipes.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final ProgressBar imageProgressBar;
        private final ShapeableImageView recipeImage;
        private final TextView recipeName;
        private final RatingBar recipeRatingBar;
        private final ImageButton recipeOptions;

        public ViewHolder(View view){
            super(view);
            imageProgressBar = view.findViewById(R.id.card_myRecipes_imageProgressBar);
            recipeImage = view.findViewById(R.id.card_myRecipes_recipeImage);
            recipeName = view.findViewById(R.id.card_myRecipes_recipeName);
            recipeRatingBar = view.findViewById(R.id.card_myRecipes_ratingBar);
            recipeOptions = view.findViewById(R.id.card_myRecipes_buttonOptions);
            //productPrice = view.findViewById(R.id.text_productPrice);
            //productFornecedor = view.findViewById(R.id.text_productFornecedor);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    public void setOnClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }


    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }



}
