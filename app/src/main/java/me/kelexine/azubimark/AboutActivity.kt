package me.kelexine.azubimark

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import kotlinx.coroutines.launch
import me.kelexine.azubimark.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding
    private val gitHubApiService = GitHubApiService.create()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply theme
        ThemeManager(this).applyTheme()
        
        // Initialize binding
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About AzubiMark"
        
        // Set up app version
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        binding.appVersion.text = "Version ${packageInfo.versionName}"
        
        // Fetch GitHub user data
        fetchGitHubUserData()
        
        // Set up GitHub link
        binding.githubButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kelexine"))
            startActivity(intent)
        }
    }
    
    private fun fetchGitHubUserData() {
        lifecycleScope.launch {
            try {
                val response = gitHubApiService.getUser("kelexine")
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        updateUIWithUserData(user)
                    }
                }
            } catch (e: Exception) {
                // Fallback to default values if API call fails
                setDefaultUserData()
            }
        }
    }
    
    private fun updateUIWithUserData(user: GitHubUser) {
        // Load avatar image
        Glide.with(this)
            .load(user.avatar_url)
            .transform(CircleCrop())
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .into(binding.userAvatar)
        
        // Update user information
        binding.userName.text = user.name ?: user.login
        binding.userBio.text = user.bio ?: getString(R.string.about_description)
        binding.userLocation.text = user.location ?: "Unknown"
        binding.userCompany.text = user.company ?: "Independent Developer"
        
        // Update stats
        binding.reposCount.text = user.public_repos.toString()
        binding.followersCount.text = user.followers.toString()
        binding.followingCount.text = user.following.toString()
        
        // Set up additional links
        binding.blogButton.setOnClickListener {
            user.blog?.let { blog ->
                val url = if (blog.startsWith("http")) blog else "https://$blog"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
        
        binding.twitterButton.setOnClickListener {
            user.twitter_username?.let { twitter ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/$twitter"))
                startActivity(intent)
            }
        }
        
        // Show/hide buttons based on available data
        binding.blogButton.visibility = if (user.blog.isNullOrEmpty()) 
            android.view.View.GONE else android.view.View.VISIBLE
        binding.twitterButton.visibility = if (user.twitter_username.isNullOrEmpty()) 
            android.view.View.GONE else android.view.View.VISIBLE
    }
    
    private fun setDefaultUserData() {
        // Fallback data if GitHub API fails
        binding.userName.text = "Franklin Kelechi"
        binding.userBio.text = getString(R.string.about_description)
        binding.userLocation.text = "Nigeria"
        binding.userCompany.text = "@azubiorg"
        
        binding.reposCount.text = "100+"
        binding.followersCount.text = "10+"
        binding.followingCount.text = "30+"
        
        // Hide optional buttons
        binding.blogButton.visibility = android.view.View.GONE
        binding.twitterButton.visibility = android.view.View.GONE
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
