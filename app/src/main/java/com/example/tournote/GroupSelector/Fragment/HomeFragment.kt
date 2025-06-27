package com.example.tournote.GroupSelector.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tournote.GroupSelector.Adapter.FetchIncludedGroupDetailsRecyclerViewAdapter
import com.example.tournote.GroupSelector.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: GroupSelectorActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.fetchGroupDetails()

        viewModel.groups.observe(viewLifecycleOwner) { groups ->
            binding.recyclerView.adapter = FetchIncludedGroupDetailsRecyclerViewAdapter(
                requireContext(), groups, viewModel
            )
        }
    }
}
