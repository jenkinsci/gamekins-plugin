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
            if (jQuery3('div.active').index() === -1) {
                let response = rsp.responseText.replace("[", "")
                response = response.replace("]", "")
                let avatars = response.split(", ")
                let avatarRows = sliceIntoChunks(avatars, 4)

                for (let i = 0; i < avatarRows.length; j++)
                {
                    let row = document.createElement("div")
                    row.classList.add("row")
                    for(j = 0; j < avatarRows[i].length; j++)
                    {
                        let item = document.createElement("div")
                        let currentAvatarSplit = jQuery3("#currentAvatar").attr("src").split("/")
                        item.classList.add("card")
                        item.classList.add("col")
                        let img = document.createElement("img")
                        let src = document.getElementsByTagName("img")[0].src
                        let base = src.substring(0, src.indexOf("static"))
                        let endSplit = src.substring(src.indexOf("static")).split("/")
                        img.src = base + "static/" + endSplit[1] + avatarRows[i][j]
                        img.classList.add("d-block")
                        img.classList.add("w-10")
                        img.style.margin = "auto"
                        item.appendChild(img)
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
    let newAvatarUrl = jQuery3('div.active')[0].firstChild.src
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
        for (let i = 0; i < chunked_arr.length; i++) {
            chunked_arr[i] = []
            for(let j = 0; j < size && i*size+j < array.length; j++) {
                  chunked_arr[i][j] = array[i*size+j]
            }
        }
        return chunked_arr
}