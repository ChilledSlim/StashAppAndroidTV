package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier


class SceneListActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            findViewById<TextView>(R.id.tag_title).text = "Scenes"
            getSupportFragmentManager().beginTransaction()
                .replace(
                    R.id.tag_fragment,
                    StashGridFragment(sceneComparator, SceneDataSupplier())
                )
                .commitNow()
        }
    }
}
