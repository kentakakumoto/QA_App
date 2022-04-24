package jp.techacademy.kenta.kakumoto.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_favorite.*

class FavoriteActivity : AppCompatActivity() {

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mFavoriteRef: DatabaseReference
    private lateinit var mQuestionRef: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: FavoriteListAdapter

    private val mFavoriteEventListener = object: ChildEventListener{
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val data = snapshot.value as Map<String, String>
            val genre = data["genre"]
            val questionUid = snapshot.key ?: ""
            Log.d("TEST", "onChildAdded genre:" + genre + "questionUid:" + questionUid)

            if (genre != null) {
                mQuestionRef =
                    mDatabaseReference.child(ContentsPATH).child(genre).child(questionUid)
                Log.d("TEST", "mQuestionRef pass")

                mQuestionArrayList.clear()
                mQuestionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        Log.d("TEST", "onDataChange mEventListener called")
                        val map = dataSnapshot.value as Map<String, String>
                        val title = map["title"] ?: ""
                        val body = map["body"] ?: ""
                        val name = map["name"] ?: ""
                        val uid = map["uid"] ?: ""
                        val imageString = map["image"] ?: ""
                        Log.d("TEST", "title: $title ,uid: $uid ,genre: $genre")
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
                                answerArrayList.add(answer)
                            }
                        }

                        val question = Question(
                            title,
                            body,
                            name,
                            uid,
                            dataSnapshot.key ?: "",
                            genre.toInt(),
                            bytes,
                            answerArrayList
                        )
                        Log.d("TEST", "FavoriteActivityでのリスト作成 title: ${question.title} ,genre: ${question.genre}")

                        mQuestionArrayList.add(question)

                        mAdapter.setQuestionArrayList(mQuestionArrayList)
                        favoriteListView.adapter = mAdapter
                        mAdapter.notifyDataSetChanged()
                        }

                    override fun onCancelled(firebaseerror: DatabaseError) {}
                })
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TEST", "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        title = getString(R.string.favorite_title)
    }

    override fun onStart(){
        super.onStart()
        //ListViewの準備
        mAdapter = FavoriteListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()

        //Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference
        val user = FirebaseAuth.getInstance().currentUser
        mFavoriteRef = mDatabaseReference.child(FavoritePATH).child(user!!.uid)
        mFavoriteRef.addChildEventListener(mFavoriteEventListener)
        Log.d("TEST", "mFavoriteEventListener pass")

        mAdapter.setQuestionArrayList(mQuestionArrayList)
        favoriteListView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        favoriteListView.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }
}
