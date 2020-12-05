const functions = require('firebase-functions');
const admin = require('firebase-admin');
const express = require('express');
const cors = require('cors');

admin.initializeApp(functions.config().firebase);
admin.auth();
const firebase = admin.database();
const firestore = admin.firestore();

const sendResponseMsg = express();
sendResponseMsg.use(cors());
sendResponseMsg.post('/:command', (req, res) => {
	const { command } = req.params;
	const { msg } = req.body;

	const ret = {
		command,
	};

	let done = false;
	firestore
		.collection('command')
		.where('command', '==', command)
		.get()
		.then(async (snapshot) => {
			try {
				if (snapshot.empty) {
					return res.status(404).json({ err: 'Command not found' });
				}

				const doc = snapshot.docs[0];
				console.log(doc.id, doc.data());

				const newDoc = await firestore
					.collection('command')
					.doc(doc.id)
					.update({ response: msg, responseTimestamp: new Date() });

				return res.status(200).json({ id: doc.id, doc: newDoc });
			} catch (err) {
				console.error(err);
				return res.status(400).json({ err: err.message });
			}
		})
		.catch((err) => {
			console.error(err);
			return res.status(400).json({ err: err.message });
		});
});
exports.sendResponseMsg = functions.https.onRequest(sendResponseMsg);

const addInQueue = express();
addInQueue.use(cors());
addInQueue.get('/:command', (req, res) => {
	const ret = {};
	const command = req.params.command;
	ret['command'] = command;
	firebase.ref('/queue').push().set(command);

	res.status(200).send(JSON.stringify(ret));
});
exports.addInQueue = functions.https.onRequest(addInQueue);
