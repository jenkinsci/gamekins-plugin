document.addEventListener("DOMContentLoaded", function () {
    let btn = jQuery3("#changeAvatarBtn")
    let descriptorFullUrl = btn.data('descriptor-url')
    let url = descriptorFullUrl + "/getCurrentAvatar"

    jQuery.ajax(url, {
        success: function (rsp) {
            let src = document.getElementsByTagName("img")[0].src
            let base = src.substring(0, src.indexOf("static"))
            let endSplit = src.substring(src.indexOf("static")).split("/")
            jQuery3("#currentAvatar").attr("src", base + "static/" + endSplit[1] + rsp)
        }
    })
})

jQuery3("#changeAvatarBtn").on("click", function () {
    let btn = jQuery3(this)
    let descriptorFullUrl = btn.data('descriptor-url')
    let url = descriptorFullUrl + "/getAvatars"

    jQuery.ajax(url, {
        success: function (rsp) {
           let avatarPicker = jQuery3("#avatar-picker")
            if (avatarPicker.children().length === 0) {
                let response = rsp.replace("[", "")
                response = response.replace("]", "")
                let avatars = response.split(", ")
                let avatarRows = sliceIntoChunks(avatars, 10)

                for (let i = 0; i < avatarRows.length; i++)
                {
                    let row = document.createElement("div")
                    row.classList.add("row")
                    row.classList.add("mt-2")
                    for(let j = 0; j < avatarRows[i].length; j++)
                    {
                        let item = document.createElement("div")
                        let cardBody = document.createElement("div")
                        cardBody.classList.add("card-body")
                        cardBody.setAttribute("role" , "button")
                        cardBody.classList.add("text-center")
                        let currentAvatarSplit = jQuery3("#currentAvatar").attr("src").split("/")
                        item.classList.add("card")
                        item.classList.add("col")
                        item.classList.add("mr-1")
                        item.classList.add("ml-1")
                        item.setAttribute("id", i + "_" + j + "-card")
                        if (avatarRows[i][j].endsWith(currentAvatarSplit[currentAvatarSplit.length - 1])) {
                            item.style.boxShadow = "0 0 1px 1px #292b2c"
                        }
                        let img = document.createElement("img")
                        let src = document.getElementsByTagName("img")[0].src
                        let base = src.substring(0, src.indexOf("static"))
                        let endSplit = src.substring(src.indexOf("static")).split("/")
                        img.src = base + "static/" + endSplit[1] + avatarRows[i][j]
                        img.classList.add("img-fluid")
                        img.classList.add("w-100")
                        img.style.margin = "auto"
                        img.classList.add("pt-2")
                        img.classList.add("pb-1")
                        cardBody.classList.add("p-0")
                        cardBody.appendChild(img)
                        item.appendChild(cardBody)
                        row.append(item)
                    }
                    avatarPicker.append(row)
                }
                jQuery3(".card").click(function () {
                    let inputElement = jQuery3(this).attr('id')
                    unclickRadio()
                    clickRadio(inputElement)
                });
            }

            jQuery3("#avatarModal").modal()
        }
    })

})

jQuery3("#chooseModalBtn").on("click", () => {
    let card;
    let cards = document.getElementsByClassName("card")
    for (let i = 0; i < cards.length; i++) {
        if (cards[i].style.boxShadow != "") {
            card = cards[i]
        }
    }
    let newAvatarUrl = card.firstChild.firstChild.src
    let split = newAvatarUrl.split("/")
    let name = split[split.length - 1]

    let btn = jQuery3("#changeAvatarBtn")
    let descriptorFullUrl = btn.data('descriptor-url')
    let url = descriptorFullUrl + "/setCurrentAvatar"

    jQuery.ajax(url, {
        data: jQuery.param({name: name}),
        success: function () {
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

function unclickRadio() {
    let cards = document.getElementsByClassName("card")
    for (let i = 0; i < cards.length; i++) {
        cards[i].style.boxShadow = ""
    }
}

function clickRadio(inputElement) {
    document.getElementById(inputElement).style.boxShadow = "0 0 1px 1px #292b2c"
}