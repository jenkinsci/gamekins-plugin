document.addEventListener("DOMContentLoaded", function () {
    let btn = jQuery3("#changeAvatarBtn")
    let descriptorFullUrl = btn.data('descriptor-url')
    let parameters = {}
    let url = descriptorFullUrl + "/getCurrentAvatar"

    new Ajax.Request(url, {
        parameters: parameters,
        onComplete: function (rsp) {
            let src = document.getElementsByTagName("img")[0].src
            let base = src.substring(0, src.indexOf("static"))
            let endSplit = src.substring(src.indexOf("static")).split("/")
            jQuery3("#currentAvatar").attr("src", base + "static/" + endSplit[1] + rsp.responseText)
        }
    })
})

jQuery3("#changeAvatarBtn").on("click", function () {
    let btn = jQuery3(this)
    let descriptorFullUrl = btn.data('descriptor-url')
    let parameters = {}
    let url = descriptorFullUrl + "/getAvatars"

    new Ajax.Request(url, {
        parameters: parameters,
        onComplete: function (rsp) {
           let avatarPicker = jQuery3("#avatar-picker")
            if (jQuery3('input[name="avatarradio"]').index() === -1) {
                let response = rsp.responseText.replace("[", "")
                response = response.replace("]", "")
                let avatars = response.split(", ")
                let avatarRows = sliceIntoChunks(avatars, 10)

                for (let i = 0; i < avatarRows.length; i++)
                {
                    let row = document.createElement("div")
                    row.classList.add("row")
                    for(let j = 0; j < avatarRows[i].length; j++)
                    {
                        let item = document.createElement("div")
                        let radio = document.createElement("input")
                        let cardbody = document.createElement("div")
                        cardbody.classList.add("card-body")
                        cardbody.appendChild(radio)
                        radio.setAttribute("type", "radio")
                        radio.setAttribute("name", "avatarradio")
                        let currentAvatarSplit = jQuery3("#currentAvatar").attr("src").split("/")
                        item.classList.add("card")
                        item.classList.add("col")
                        if (avatarRows[i][j].endsWith(currentAvatarSplit[currentAvatarSplit.length - 1])) {
                            radio.checked = true
                        } else {
                            radio.checked = false
                        }

                        let img = document.createElement("img")
                        let src = document.getElementsByTagName("img")[0].src
                        let base = src.substring(0, src.indexOf("static"))
                        let endSplit = src.substring(src.indexOf("static")).split("/")
                        img.src = base + "static/" + endSplit[1] + avatarRows[i][j]
                        img.classList.add("d-block")
                        img.classList.add("w-10")
                        img.classList.add("img-fluid")
                        img.style.margin = "auto"
                        item.appendChild(img)
                        item.appendChild(cardbody)
                        row.append(item)
                    }
                    avatarPicker.append(row)
                }
            }

            jQuery3("#avatarModal").modal()
        }
    })

})

jQuery3("#chooseModalBtn").on("click", () => {
    let newAvatarUrl = jQuery3('input[name=avatarradio]:checked')[0].parentElement.parentElement.firstChild.src
    let split = newAvatarUrl.split("/")
    let name = split[split.length - 1]

    let btn = jQuery3("#changeAvatarBtn")
    let descriptorFullUrl = btn.data('descriptor-url')
    let parameters = {}
    parameters["name"] = name
    let url = descriptorFullUrl + "/setCurrentAvatar"

    new Ajax.Request(url, {
        parameters: parameters,
        onComplete: function () {
            jQuery3("#currentAvatar").attr("src", newAvatarUrl)
        }
    })
})

function sliceIntoChunks(array, size) {
    let chunked_arr = []
        for (let i = 0; i < array.length/size; i++) {
            chunked_arr[i] = []
            for(let j = 0; j < size && i*size+j < array.length; j++) {
                chunked_arr[i][j] = array[i*size+j]
            }
        }
        return chunked_arr
}