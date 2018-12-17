const functions = require('firebase-functions')
const admin = require('firebase-admin')
const express = require('express')
const cors = require('cors')

admin.initializeApp(functions.config().firebase)
admin.auth()
const firebase = admin.database();
const firestore = admin.firestore();

const sendResponseMsg = express()
sendResponseMsg.use(cors())
sendResponseMsg.post('/:command', (req, res) => {
	var ret = {}

	var command = req.params.command													;ret['command'] = command
	var msg = req.body.msg

	var done = false
	firestore.collection('command')
		.where('command', '==', command)
		.onSnapshot(docSnapshot => {
			if (done) return
			done = true

			try{
				docSnapshot.docChanges().forEach(change => {
					var data = change.doc.data()
					var id = change.doc.id												;ret['id'] = id
					data.response = msg													;ret['response'] = msg
					data.responseTimestamp = new Date()									;ret['responseTimestamp'] = data.responseTimestamp
					firestore.collection('command').doc(id).set(data)
				})
			} catch(err){
				ret['err'] = err.message
			}

			res.status(200).send(JSON.stringify(ret))
		}, err => {
			res.status(400).send(err)
		});
})
exports.sendResponseMsg = functions.https.onRequest(sendResponseMsg)


const addInQueue = express()
addInQueue.use(cors())
addInQueue.get('/:command', (req, res) => {
	var ret = {}
	var command = req.params.command													;ret['command'] = command
	firebase.ref('/queue').push().set(command)

	res.status(200).send(JSON.stringify(ret))
})
exports.addInQueue = functions.https.onRequest(addInQueue)