package jp.techacademy.kenta.kakumoto.qa_app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCreateAccountListener: OnCompleteListener<AuthResult>
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    private lateinit var mDataBaseReference: DatabaseReference

    //アカウント作成時にフラグをたて、ログイン処理後に名前をFirebaseに保存する
    private var mIsCreateAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mDataBaseReference = FirebaseDatabase.getInstance().reference

        //FirebaseAuthのオブジェクトを取得
        mAuth = FirebaseAuth.getInstance()

        //アカウント作成処理のリスナー
        mCreateAccountListener = OnCompleteListener { task ->
            if(task.isSuccessful){
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                login(email, password)
            }else{
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.create_account_failure_message), Snackbar.LENGTH_LONG).show()

                progressBar.visibility = View.GONE //プログレスバー非表示
            }
        }

        //ログイン処理のリスナー
        mLoginListener = OnCompleteListener { task ->
            if(task.isSuccessful){
                val user = mAuth.currentUser
                val userRef = mDataBaseReference.child(UsersPATH).child(user!!.uid)

                if(mIsCreateAccount){ //アカウント作成時はFirebaseに表示名を保存
                    val name = nameText.text.toString()
                    val data = HashMap<String, String>()
                    data["name"] = name
                    userRef.setValue(data)
                    saveName(name) //表示名をPreferenceに保存
                }else{
                    userRef.addListenerForSingleValueEvent(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot){
                            val data = snapshot.value as Map<*,*>?
                            saveName(data!!["name"] as String)
                        }
                        override fun onCancelled(firebaseError: DatabaseError){}
                    })
                }
                progressBar.visibility = View.GONE

                finish() //Activityを閉じる
            }else{
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.login_failure_message), Snackbar.LENGTH_LONG).show()

                progressBar.visibility = View.GONE
            }
        }

        //タイトル設定
        title = getString(R.string.login_title)

        createButton.setOnClickListener{ v ->
            //キーボードが出ていたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text. toString()

            if(email.length != 0 && password.length >= 6 && name.length != 0){
                mIsCreateAccount = true
                Log.d("TEST", "mIsCreateAccount = true")
                createAccount(email, password)
            }else{
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show()
            }
        }

        loginButton.setOnClickListener { v ->
            //キーボードが出ていたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            if(email.length != 0 && password.length >= 6) {
                mIsCreateAccount = false
                login(email, password)
            }else{
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    //アカウント作成
    private fun createAccount(email:String, password:String){
        progressBar.visibility = View.VISIBLE
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListener)
    }

    //ログイン
    private fun login(email:String, password: String){
        progressBar.visibility = View.VISIBLE
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener)
    }

    //表示名をPreferenceに保存
    private fun saveName(name: String){
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        editor.commit()
    }

}