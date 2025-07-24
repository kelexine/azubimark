package me.kelexine.azubimark

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class OutlineAdapter(
    private val context: Context,
    private val onItemClick: (OutlineItem) -> Unit
) : RecyclerView.Adapter<OutlineAdapter.OutlineViewHolder>() {

    private var items = listOf<OutlineItem>()
    private var currentItemPosition = -1

    data class OutlineItem(
        val text: String,
        val level: Int,
        val position: Int,
        val anchor: String? = null
    )

    class OutlineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: MaterialCardView = itemView as MaterialCardView
        val levelIndicator: View = itemView.findViewById(R.id.level_indicator)
        val headingIcon: ImageView = itemView.findViewById(R.id.heading_icon)
        val headingText: TextView = itemView.findViewById(R.id.heading_text)
        val pageIndicator: TextView = itemView.findViewById(R.id.page_indicator)
        val currentIndicator: ImageView = itemView.findViewById(R.id.current_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutlineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_outline_heading, parent, false)
        return OutlineViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutlineViewHolder, position: Int) {
        val item = items[position]
        
        // Configure text
        holder.headingText.text = item.text
        
        // Configure heading level styling
        configureHeadingLevel(holder, item.level)
        
        // Configure current section indicator
        if (position == currentItemPosition) {
            holder.currentIndicator.visibility = View.VISIBLE
            holder.container.strokeWidth = 2
            holder.container.strokeColor = context.getColor(R.color.md_theme_light_primary)
        } else {
            holder.currentIndicator.visibility = View.GONE
            holder.container.strokeWidth = 0
        }
        
        // Set click listener
        holder.container.setOnClickListener {
            onItemClick(item)
            setCurrentItem(position)
        }
        
        // Configure accessibility
        holder.container.contentDescription = context.getString(
            R.string.heading_level_content_description, 
            item.level, 
            item.text
        )
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<OutlineItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setCurrentItem(position: Int) {
        val oldPosition = currentItemPosition
        currentItemPosition = position
        
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition)
        }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    private fun configureHeadingLevel(holder: OutlineViewHolder, level: Int) {
        // Set indentation based on heading level
        val params = holder.container.layoutParams as ViewGroup.MarginLayoutParams
        val baseMargin = context.resources.getDimensionPixelSize(R.dimen.spacing_md)
        val levelIndent = context.resources.getDimensionPixelSize(R.dimen.outline_indent_step)
        
        params.marginStart = baseMargin + (level - 1) * levelIndent
        holder.container.layoutParams = params
        
        // Configure level indicator
        when (level) {
            1 -> {
                holder.levelIndicator.visibility = View.VISIBLE
                holder.levelIndicator.setBackgroundColor(
                    ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorPrimary)
                )
                holder.headingText.textSize = 16f
                holder.headingText.typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            2 -> {
                holder.levelIndicator.visibility = View.VISIBLE
                holder.levelIndicator.setBackgroundColor(
                    ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorSecondary)
                )
                holder.headingText.textSize = 15f
                holder.headingText.typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            3 -> {
                holder.levelIndicator.visibility = View.GONE
                holder.headingText.textSize = 14f
                holder.headingText.typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            else -> {
                holder.levelIndicator.visibility = View.GONE
                holder.headingText.textSize = 13f
                holder.headingText.typeface = android.graphics.Typeface.DEFAULT
            }
        }
        
        // Set heading icon based on level
        val iconRes = when (level) {
            1 -> R.drawable.ic_heading_1
            2 -> R.drawable.ic_heading_2
            3 -> R.drawable.ic_heading_3
            else -> R.drawable.ic_outline
        }
        
        holder.headingIcon.setImageResource(iconRes)
        
        // Set alpha based on level for visual hierarchy
        val alpha = when (level) {
            1 -> 1.0f
            2 -> 0.9f
            3 -> 0.8f
            else -> 0.7f
        }
        
        holder.headingText.alpha = alpha
        holder.headingIcon.alpha = alpha
    }
}