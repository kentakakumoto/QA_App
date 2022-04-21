package jp.techacademy.kenta.kakumoto.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoriteRef: DatabaseReference
    private var isFavorite: Boolean = false

    private val mEventListener = object: ChildEventListener{
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers){
                //同じAnswerUidののものが存在しているときは何もしない
                if(answerUid == answer.answerUid){
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {}
    }

    private val mFavoriteEventListener = object: ChildEventListener{
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            Log.d("TEST","mFavoriteEventListener called")
            isFavorite = true
            Log.d("TEST","isFavorite is true")
            favoriteImageView?.setImageResource(R.drawable.ic_favorite)

            favoriteImageView?.setOnClickListener {
                    mFavoriteRef.removeValue()
                    isFavorite = false
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onChildRemoved(snapshot: DataSnapshot) {
            favoriteImageView?.setImageResource(R.drawable.ic_favorite_border)
            Log.d("TEST","onChildRemoved called")

            favoriteImageView?.setOnClickListener {
                    val data = HashMap<String, String>()
                    data["genre"] = mQuestion.genre.toString()
                    mFavoriteRef.setValue(data)
                    isFavorite = true
            }
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)
    }

    override fun onResume(){
        Log.d("TEST","onResume")
        super.onResume()

        //渡ってきたQuestionのオブジェクトを保持
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        //ListView準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener{
            val user = FirebaseAuth.getInstance().currentUser
            if(user == null){
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            }else{
                //Questionを渡して解答作成画面へ
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val databaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = databaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        var user = FirebaseAuth.getInstance().currentUser

        if(user != null) {
            Log.d("TEST","user is not null")
            favoriteImageView?.visibility = View.VISIBLE
            mFavoriteRef = databaseReference.child(FavoritePATH).child(user.uid).child(mQuestion.questionUid)
            mFavoriteRef.addChildEventListener(mFavoriteEventListener)

            favoriteImageView?.setImageResource(R.drawable.ic_favorite_border)
            favoriteImageView?.setOnClickListener {
                favoriteImageView?.setImageResource(R.drawable.ic_favorite)
                val data = HashMap<String, String>()
                data["genre"] = mQuestion.genre.toString()
                mFavoriteRef.setValue(data)
                isFavorite = true
            }
        } else{
            favoriteImageView?.visibility = View.INVISIBLE
        }
    }
}