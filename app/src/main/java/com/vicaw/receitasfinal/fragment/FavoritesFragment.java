package com.vicaw.receitasfinal.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vicaw.receitasfinal.R;
import com.vicaw.receitasfinal.adapter.RecipesListAdapter;
import com.vicaw.receitasfinal.databinding.FragmentFavoritesBinding;
import com.vicaw.receitasfinal.model.Recipe;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;


public class FavoritesFragment extends Fragment {

    private FragmentFavoritesBinding binding;

    private DatabaseReference recipesRef;
    private DatabaseReference favoritesRef;

    private RecyclerView recipesRecycler;
    private ProgressBar progressBar;
    private TextView emptyText;
    private RecipesListAdapter recipesListAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipesRef = FirebaseDatabase.getInstance().getReference().child("recipes");
        favoritesRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("favorites");


    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        progressBar = binding.favoritesProgressBar;
        recipesRecycler = binding.favoritesRecipesRecycler;
        emptyText = binding.favoritesEmptyText;

        recipesListAdapter = new RecipesListAdapter(getContext(), new ArrayList<>());
        recipesRecycler.setAdapter(recipesListAdapter);
        recipesRecycler.setHasFixedSize(true);
        recipesRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2, GridLayoutManager.VERTICAL, false));

        recipesListAdapter.setOnClickListener((view, position) -> {
            Bundle bundle = new Bundle();
            String recipeId = recipesListAdapter.getItem(position).getId();
            bundle.putString("recipeId", recipeId);

            Navigation.findNavController(view).navigate(R.id.action_navigation_favorites_to_navigation_recipe, bundle);
        });

        getRecipesFromDB();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void getRecipesFromDB(){
        recipesRecycler.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        //Somente com o firebase não é possível fazer consultas muito complexas.
        //Outra opção para diminuir o numero de requisições seria transferir
        //a lista de favoritos para a tabela das receitas e utilizar algo como
        //receitasRef.child("likedBy).orderByKey().equalTo(firebaseAuth.getCurrentUser().getUid())

        favoritesRef.get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.getChildrenCount() > 0) {

                AtomicInteger removedCount = new AtomicInteger();

                for (DataSnapshot favoriteData: dataSnapshot.getChildren()) {
                    String recipeId = favoriteData.getKey();
                    recipesRef.child(recipeId).get().addOnSuccessListener(recipeData -> {
                        Recipe recipe = recipeData.getValue(Recipe.class);

                        //Se for null, a receita foi excluída e ela deve ser removida da lista de favoritos.
                        if (recipe == null) {
                            favoritesRef.child(recipeId).removeValue();
                            removedCount.getAndIncrement();
                        } else {
                            recipe.setId(recipeData.getKey());
                            recipesListAdapter.addToList(recipe);
                        }

                        long dataSize = dataSnapshot.getChildrenCount() - removedCount.get();

                        //Se todas as receitas já foram adicionadas
                        if (recipesListAdapter.getItemCount() == dataSize){
                            recipesRecycler.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);

                            if (dataSize == 0) {
                                emptyText.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }

            } else {
                emptyText.setVisibility(View.VISIBLE);
                recipesRecycler.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }

        });
    }

}