package com.vicaw.receitasfinal.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vicaw.receitasfinal.R;
import com.vicaw.receitasfinal.adapter.MyRecipesListAdapter;
import com.vicaw.receitasfinal.databinding.FragmentMyRecipesBinding;
import com.vicaw.receitasfinal.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class MyRecipesFragment extends Fragment {

    private FragmentMyRecipesBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference recipesRef;

    private RecyclerView recipesRecycler;
    private ProgressBar progressBar;

    private TextView emptyText;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        recipesRef = FirebaseDatabase.getInstance().getReference().child("recipes");

    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMyRecipesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Button buttonNewRecipe = binding.myRecipesButtonNewRecipe;
        recipesRecycler = binding.myRecipesRecipesRecycler;
        progressBar = binding.myRecipesProgressBar;
        emptyText = binding.myRecipesEmptyText;

        buttonNewRecipe.setOnClickListener(view -> {
            Navigation.findNavController(root).navigate(R.id.action_navigation_myRecipes_to_navigation_newRecipe);
        });


        recipesRecycler.setAdapter(new MyRecipesListAdapter(getContext(), new ArrayList<>()));
        recipesRecycler.setHasFixedSize(true);
        recipesRecycler.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        recipesRecycler.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));

        ((MyRecipesListAdapter) recipesRecycler.getAdapter()).setOnClickListener((view, position) -> {
            Bundle bundle = new Bundle();
            String recipeId = ((MyRecipesListAdapter) recipesRecycler.getAdapter()).getItem(position).getId();
            bundle.putString("recipeId", recipeId);

            Navigation.findNavController(view).navigate(R.id.action_navigation_myRecipes_to_navigation_recipe, bundle);
        });

        getMyRecipesFromDB();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void getMyRecipesFromDB(){
        recipesRef.orderByChild("postedBy")
                .equalTo(firebaseAuth.getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Recipe> recipes = new ArrayList<>();

                        for (DataSnapshot recipeData: snapshot.getChildren()) {
                            Recipe recipe = recipeData.getValue(Recipe.class);
                            recipe.setId(recipeData.getKey());
                            recipes.add(recipe);
                        }

                        if(recipes.size() == 0){
                            emptyText.setVisibility(View.VISIBLE);
                        }

                        ((MyRecipesListAdapter) recipesRecycler.getAdapter()).setList(recipes);
                        progressBar.setVisibility(View.GONE);

                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

    }



}