package com.vicaw.receitasfinal.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.vicaw.receitasfinal.R;
import com.vicaw.receitasfinal.adapter.RecipesListAdapter;
import com.vicaw.receitasfinal.databinding.FragmentHomeBinding;
import com.vicaw.receitasfinal.model.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private DatabaseReference recipesRef;
    private ProgressBar progressBar;

    private RecyclerView recipesRecycler;

    private RecipesListAdapter recipesListAdapter;

    private List<Recipe> currentList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipesRef = FirebaseDatabase.getInstance().getReference().child("recipes");

        initAdapter();
        getRecipesFromDB();

    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recipesRecycler = binding.homeRecipesRecycler;
        progressBar = binding.homeProgressBar;
        SearchView searchView = binding.homeSearchView;

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                List<Recipe> filteredList = currentList.stream()
                        .filter(recipe -> recipe.getName().toLowerCase().contains(newText.toLowerCase()) ||
                                        recipe.getIngredients().toLowerCase().contains(newText.toLowerCase()))
                        .collect(Collectors.toList());

                recipesListAdapter.setList(filteredList);

                recipesRecycler.scrollToPosition(0);
                return true;
            }
        });

        if(recipesListAdapter.getItemCount() != 0){
            progressBar.setVisibility(View.GONE);
        }

        recipesRecycler.setAdapter(recipesListAdapter);
        recipesRecycler.setHasFixedSize(true);
        recipesRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2, GridLayoutManager.VERTICAL, false));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    private void initAdapter(){
        recipesListAdapter = new RecipesListAdapter(getContext(), new ArrayList<>());
        recipesListAdapter.setOnClickListener((view, position) -> {
            Bundle bundle = new Bundle();
            String recipeId = ((RecipesListAdapter) recipesRecycler.getAdapter()).getItem(position).getId();
            bundle.putString("recipeId", recipeId);

            Navigation.findNavController(view).navigate(R.id.action_navigation_home_to_navigation_recipe, bundle);
        });
    }

    private void getRecipesFromDB(){
        recipesRef.get().addOnSuccessListener(dataSnapshot -> {
            List<Recipe> recipes = new ArrayList<>();

            for (DataSnapshot recipeData: dataSnapshot.getChildren()) {
                Recipe recipe = recipeData.getValue(Recipe.class);
                recipe.setId(recipeData.getKey());
                recipes.add(recipe);
            }

            currentList = recipes;
            recipesListAdapter.setList(currentList);
            progressBar.setVisibility(View.GONE);
        });

    }

}