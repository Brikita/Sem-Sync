package com.example.semsync

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.Timestamp
import com.example.semsync.databinding.FragmentGroupDetailBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

class GroupDetailFragment : Fragment() {

    private var _binding: FragmentGroupDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val posts = mutableListOf<GroupPost>()
    private lateinit var adapter: PostsAdapter
    private var groupId: String? = null
    private var groupName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve args from bundle
        arguments?.let {
            groupId = it.getString("groupId")
            groupName = it.getString("groupName")
        }

        binding.textGroupDetailHeader.text = groupName ?: "Group Details"

        // Initialize RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PostsAdapter(posts)
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.fab_members).setOnClickListener {
            showMembersDialog()
        }

        view.findViewById<View>(R.id.fab_new_post).setOnClickListener {
            // Updated: Create a dialog for typing post content
            showCreatePostDialog()
        }
        
        if (groupId != null) {
            fetchPosts()
        }
    }

    private fun showMembersDialog() {
        if (groupId == null) return

        db.collection("groups").document(groupId!!).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Corrected: Explicitly specify the type for emptyList
                    val members = document.get("members") as? List<String> ?: emptyList<String>()
                    val memberCount = members.size
                    
                    // Simple dialog for now showing count.
                    AlertDialog.Builder(requireContext())
                        .setTitle("Group Members")
                        .setMessage("Total Members: $memberCount\n\n(List view requires advanced queries)")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
    }

    private fun showCreatePostDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_post, null)
        val input = dialogView.findViewById<EditText>(R.id.edit_post_content)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Create Post")
            .setView(dialogView)
            .setPositiveButton("Post") { _, _ ->
                val content = input.text.toString().trim()
                if (content.isNotEmpty()) {
                    createPost(content)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createPost(content: String) {
        val user = auth.currentUser ?: return
        val newPost = GroupPost(
            groupId = groupId!!,
            authorId = user.uid,
            authorName = user.displayName ?: "Student",
            content = content,
            createdAt = Timestamp.now(),
            type = "announcement"
        )
        
        db.collection("groups").document(groupId!!)
            .collection("posts")
            .add(newPost)
            .addOnSuccessListener {
                Toast.makeText(context, "Post created!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to post", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchPosts() {
        db.collection("groups").document(groupId!!)
            .collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("GroupDetailFragment", "Listen failed.", error)
                    return@addSnapshotListener
                }

                posts.clear()
                for (doc in value!!) {
                    val post = doc.toObject(GroupPost::class.java).copy(id = doc.id)
                    posts.add(post)
                }
                adapter.notifyDataSetChanged()
            }
    }

    inner class PostsAdapter(private val posts: List<GroupPost>) :
        RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

        inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val authorName: TextView = itemView.findViewById(R.id.text_author_name)
            val postDate: TextView = itemView.findViewById(R.id.text_post_date)
            val postType: TextView = itemView.findViewById(R.id.text_post_type)
            val content: TextView = itemView.findViewById(R.id.text_post_content)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_post, parent, false)
            return PostViewHolder(view)
        }

        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            val post = posts[position]
            holder.authorName.text = post.authorName
            holder.postDate.text = post.createdAt?.toDate()?.toString() ?: "Just now"
            // Updated deprecated capitalize() to modern equivalent
            holder.postType.text = post.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            holder.content.text = post.content
        }

        override fun getItemCount() = posts.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}