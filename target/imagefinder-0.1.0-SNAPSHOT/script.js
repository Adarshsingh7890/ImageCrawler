var resultList = document.querySelector('ul.results');
var urlInput = document.querySelector('input[name=url]')

apiCallBack = function(xhr, callback) {
    if (xhr.readyState == XMLHttpRequest.DONE) {
        if (xhr.status != 200) {
            let message = xhr.status + ":" + xhr.statusText + ":"
                    + xhr.responseText;
            alert(message);
            throw 'API call returned bad code: ' + xhr.status;
        }
        let response = xhr.responseText ? JSON.parse(xhr.responseText)
                : null;
        if (callback) {
            callback(response);
        }
    }
}

updateList = function(response) {
    resultList.innerHTML = '';
    for (var i = 0; i < response.length; i++) {
        var img = document.createElement("img");
        img.width = 200;
        img.src = response[i];
        resultList.appendChild(img);
    }
}

makeApiCall = function(url, method, obj, callback) {
    let xhr = new XMLHttpRequest();
    xhr.open(method, url);
    xhr.onreadystatechange = apiCallBack.bind(null, xhr, callback);
    xhr.send(obj ? obj instanceof FormData || obj.constructor == String ? obj : JSON.stringify(obj) : null);
}

document.querySelector('button').addEventListener("click", function(event) {
    event.preventDefault();
    makeApiCall('/main?url=' + urlInput.value, 'POST', null, updateList);
});