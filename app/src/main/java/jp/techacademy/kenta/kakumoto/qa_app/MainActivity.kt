package jp.techacademy.kenta.kakumoto.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var mGenre = 0 //5.5

    //8.5 ListView
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    private var mGenreRef: DatabaseReference? = null

    private val mEventListener = object: ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }
            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    //Log.d("TEST", "onChildAdded answer"+answer.toString())
                    answerArrayList.add(answer) 
                }
            }

            val question = Question(
                title,
                body,
                name,
                uid,
                dataSnapshot.key ?: "",
                mGenre,
                bytes,
                answerArrayList
            )
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            //変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    //このアプリで変更がある可能性があるのはAnswerのみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //4.6 idがtoolbarのインポート宣言により取得されているので、id名のActionbarの作成を依頼
        setSupportActionBar(toolbar)

        //4.6 fabのClickListener登録、未ログインならLoginにIntent
        fab.setOnClickListener{ view ->
            //7.6 ジャンルを選択していない場合はエラー
            if(mGenre == 0){
                Snackbar.make(view, getString(R.string.question_no_select_genre), Snackbar.LENGTH_LONG).show()
            }else{

            }

            val user = FirebaseAuth.getInstance().currentUser

            if(user == null){
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            }else{ //7.6
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
        }

        //5.5　ナビゲーションドロワーの設定
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.app_name, R.string.app_name)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

            //8.5
            //Firebase
            mDatabaseReference = FirebaseDatabase.getInstance().reference

            //ListViewの準備
            mAdapter = QuestionsListAdapter(this)
            mQuestionArrayList = ArrayList<Question>()
            mAdapter.notifyDataSetChanged()

            listView.setOnItemClickListener { parent, view, position, id ->
                val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
                intent.putExtra("question", mQuestionArrayList[position])
                startActivity(intent)
            }
    }

    //7.6 趣味を既定の選択とする
    override fun onResume(){
        super.onResume()
        //val navigationView = findViewById<NavigationView>(R.id.nav_view) //8.5で削除

        if(mGenre == 0){
            onNavigationItemSelected(nav_view.menu.getItem(0))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //6.3
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if(id == R.id.action_settings){
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    //5.5
    override fun onNavigationItemSelected(item: MenuItem): Boolean{
        val id = item.itemId

        if(id == R.id.nav_hobby){
            toolbar.title = getString(R.string.menu_hobby_label)
            mGenre = 1
        }else if(id == R.id.nav_life){
            toolbar.title = getString(R.string.menu_life_label)
            mGenre = 2
        }else if(id == R.id.nav_health){
            toolbar.title = getString(R.string.menu_health_label)
            mGenre = 3
        }else if(id == R.id.nav_computer){
            toolbar.title = getString(R.string.menu_computer_label)
            mGenre = 4
        }

        drawer_layout.closeDrawer(GravityCompat.START)

        //8.5 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットしなおす
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        //8.5 選択したジャンルにリスナーを登録
        if(mGenreRef != null){
            mGenreRef!!.removeEventListener(mEventListener)
        }
        mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
        mGenreRef!!.addChildEventListener(mEventListener)
        return true
    }
}