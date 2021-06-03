"""
The webserver which facilitates vr requests and management
uses Flask and Pymongo
"""
from flask import Flask, request, url_for, jsonify, redirect
from pymongo import MongoClient
from bson.objectid import ObjectId

# prep the app
app = Flask(__name__)
prefix = "/final"
# app root wasn't working for me, so i just use a string literal and will insert into all routes
# not the perfect solution, but i dont really care that much

# connecting to mongo
client = MongoClient()
db = client.request_db
users = db.user_collection
requests = db.requests

# testing functions

@app.route(prefix+'/')
def test():
    # http://localhost:5000/final/
    return "this is working"

@app.route(prefix+'/count/users')
def count_users():
    # http://localhost:5000/final/count/users
    return "# of users = "+str(users.count_documents({}))

@app.route(prefix+'/count/requests')
def count_requests():
    # http://localhost:5000/final/count/requests
    return "# of requests = "+str(requests.count_documents({}))

@app.route(prefix+'/test/create_user/<name>')
def create_user_test(name):
    user = {"username":name,"tag":"test_user"}
    uid = users.insert_one(user).inserted_id
    return redirect(url_for('find_by_id',id=str(uid)))

@app.route(prefix+'/test/find_user_by_id/<id>')
def find_by_id(id):
    user = users.find_one({"_id":ObjectId(id)}) 
    return jsonify(fix_id(user))

@app.route(prefix+'/test/find_user_by_name/<name>')
def find_by_name(name):
    user = users.find_one({"username":name})
    return jsonify(fix_id(user))

@app.route(prefix+'/view/users_list')
def get_all_users():
    # http://localhost:5000/final/view/users_list
    userlist = list(users.find())
    return jsonify(fix_id(userlist))

@app.route(prefix+"/view/requests_list")
def get_all_requests():
    # http://localhost:5000/final/view/requests_list
    requestlist = list(requests.find())
    for x in requestlist:
        x["user"]=fix_id(x["user"])
    return jsonify(fix_id(requestlist))

# client side urls
clprefix = prefix+"/client"

@app.route(clprefix+"/cancel/<id>", methods=["GET","POST","PUT"])
def client_cancel(id):
    requests.update_one(
        {"user._id":ObjectId(id),"status":"open"},
        {"$set":{"status":"canceled"}})
    return "request canceled"

@app.route(clprefix+"/test/request/<id>")
def client_request_test(id):
    if(requests.count_documents({"user._id":ObjectId(id)}) > 0):
        return "user already in queue"
    else:
        user = users.find_one({"_id":ObjectId(id)})
        req = {"user":user,"time":20,"status":"open"}
        rid = requests.insert_one(req).inserted_id
        return(str(rid))

@app.route(clprefix+"/request",methods=['POST'])
def client_request():
    json = request.get_json(silent=True) # this comes in as dict
    id = ObjectId(json["uid"])
    status = "unknown"
    rid = "unkown"
    if(requests.count_documents({"user._id":id,"status":"open"}) > 0):
        req = requests.find_one({"user._id":id,"status":"open"})
        status = "already open"
        rid = str(req["_id"])
    else:
        user = users.find_one({"_id":id})
        req = {"user":user,"time":json["time"],"status":"open"}
        rid = str(requests.insert_one(req).inserted_id)
        status = "added"
    return jsonify({"status":status,"rid":rid})

@app.route(clprefix+"/add_new_user",methods=['POST'])
def add_client():
    json = request.get_json(silent=True)
    user = {"username":json["username"],"tag":json["device"]}
    # going to keep track of which device sent the user
    uid = users.insert_one(user).inserted_id
    return jsonify({"uid":str(uid)}) #returning this id back to device so that it can query by uid

@app.route(clprefix+"/update_user",methods=['PUT'])
def update_client():
    json = request.get_json(silent=True)
    uid = ObjectId(json["uid"])
    uname = json["username"]
    users.update_one(
        {"_id":uid},
        {"$set":{"username":uname}}
    )
    requests.update_one(
        {"user._id":uid,"status":"open"},
        {"$set":{"user.username":uname}}
    )
    return jsonify({"response":"good"})

@app.route(clprefix+"/check_status/<rid>",methods=["GET"])
def check_request_status(rid):
    req = requests.find_one({"_id":ObjectId(rid)})
    return req["status"]

# admin urls
adprefix = prefix+"/admin"

@app.route(adprefix+"/get_open_requests", methods=['GET'])
def get_open_requests():
    # http://localhost:5000/final/admin/get_open_requests
    openReqs = list(requests.find({"status":"open"}))
    for x in openReqs:
        x["user"]=fix_id(x["user"])
    return jsonify(fix_id(openReqs))

@app.route(adprefix+"/approve/<uid>",methods=['POST'])
def approve_user(uid):
    uid = ObjectId(uid)
    return on_status_change(uid,"approved")

@app.route(adprefix+"/deny/<uid>",methods=['POST'])
def deny_user(uid):
    uid = ObjectId(uid)
    return on_status_change(uid,"denied")

# some helper functions    
def fix_id(document):
    """a helper function"""
    if(type(document) is dict):
        document["_id"]=str(document["_id"])
    if(type(document) is list):
        for x in document:
            x["_id"]=str(x["_id"])
    return document #moving jsonify out of this

def on_status_change(uid, status):
    """a function which will send a notif when the user's request changes status"""
    requests.update_one(
        {"user._id":uid,"status":"open"},
        {"$set":{"status":status}})
    return "request "+status



if __name__ == "__main__":
    app.run(host='0.0.0.0')  # so that we can connect from other devices