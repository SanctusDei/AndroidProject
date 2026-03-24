package fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.milkanalytic.R;
import com.example.milkanalytic.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();

    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvTodayValue.setText("5");

        binding.btnStartAnalysis.setOnClickListener( v-> {

            Navigation.findNavController(v).navigate(R.id.action_home_to_analysis);

        });

        setupRecentlist();

    }

    // TODO
    private void setupRecentlist() {

    }


    @Override
    public void onDestroyView() {

        super.onDestroyView();
        binding = null;

    }
}