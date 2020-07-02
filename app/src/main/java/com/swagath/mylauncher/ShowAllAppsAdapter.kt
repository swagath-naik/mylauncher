package com.swagath.mylauncher

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.app_holder.view.*

class ShowAllAppsAdapter :
    ListAdapter<AppInfoModel, ShowAllAppsAdapter.AppInfoModelViewHolder>(AppInfoModelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppInfoModelViewHolder {
        return AppInfoModelViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: AppInfoModelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AppInfoModelViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: AppInfoModel) = with(itemView) {
            this.app_name.text = item.package_label
            val icon: Drawable? =
                context.packageManager.getApplicationIcon(item.package_name)
            this.app_icon.setImageDrawable(icon)
            this.setOnClickListener {
                val launchIntent =
                    context.packageManager.getLaunchIntentForPackage(item.package_name)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            }
        }

        //For inflating the layout in onCreateViewHolder()
        companion object {
            fun from(parent: ViewGroup): AppInfoModelViewHolder {
                val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.app_holder, parent, false)
                return AppInfoModelViewHolder(view)
            }
        }
    }
}

class AppInfoModelDiffCallback() : DiffUtil.ItemCallback<AppInfoModel>() {
    override fun areItemsTheSame(oldItem: AppInfoModel, newItem: AppInfoModel): Boolean {
        // Confirm that your id variable matches this one or change this one to match
        //the one in your model
        return oldItem.package_name == newItem.package_name
    }

    override fun areContentsTheSame(oldItem: AppInfoModel, newItem: AppInfoModel): Boolean {
        return oldItem == newItem
    }
}