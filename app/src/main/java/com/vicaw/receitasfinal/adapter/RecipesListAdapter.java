package com.vicaw.receitasfinal.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
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

public class RecipesListAdapter extends RecyclerView.Adapter<RecipesListAdapter.ViewHolder> {

    private final Context context;
    private List<Recipe> recipes;

    private ItemClickListener mClickListener;

    public RecipesListAdapter(Context context, List<Recipe> recipes) {
        this.context = context;
        this.recipes = recipes;
    }

    public RecipesListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_recipe, parent, false);
        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        DatabaseReference authorRef = dbRef
                .child("users")
                .child(recipe.getPostedBy());

        DatabaseReference recipesRatingRef = dbRef.child("recipes")
                .child(recipe.getId())
                .child("ratedBy");

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference imagens = storageReference.child("recipes/" + recipe.getImageName());



        //holder.imageProgressBar.setVisibility(View.VISIBLE);
       // holder.recipeImage.setVisibility(View.GONE);

        Glide.with(context)
                .load(imagens).listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        //holder.imageProgressBar.setVisibility(View.GONE);
                      //  holder.recipeImage.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(holder.recipeImage);

        holder.recipeName.setText(recipe.getName());

        authorRef.child("name").get()
                .addOnSuccessListener(data -> {
                    String authorName = data.getValue(String.class);
                    holder.recipeAuthor.setText(context.getString(R.string.recipe_author, authorName));

                });

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


    }


    public void setList(List<Recipe> recipes){
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    public void addToList(Recipe recipe){
        this.recipes.add(recipe);
        notifyDataSetChanged();
    }

    public List<Recipe> getCurrentList(){
        return recipes;
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
        //private final ProgressBar imageProgressBar;
        private final ImageView recipeImage;
        private final TextView recipeName;
        private final TextView recipeAuthor;
        private final RatingBar recipeRatingBar;
        public ViewHolder(View view){
            super(view);
            //imageProgressBar = view.findViewById(R.id.card_myRecipes_imageProgressBar);
            recipeImage = view.findViewById(R.id.recipeCard_image);
            recipeName = view.findViewById(R.id.recipeCard_name);
            recipeAuthor = view.findViewById(R.id.recipeCard_author);
            recipeRatingBar = view.findViewById(R.id.recipeCard_ratingBarIndicator);

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
