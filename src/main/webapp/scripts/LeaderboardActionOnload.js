window.onload = (function () {
    prettyPrint();
    jQuery3('[data-toggle="tooltip"]').tooltip()

    let avatars = jQuery3('[id^=imageUser]')
    let src = document.getElementsByTagName("img")[0].src
    let base = src.substring(0, src.indexOf("static"))
    let endSplit = src.substring(src.indexOf("static")).split("/")
    for (let avatar of avatars) {
        let split = avatar.src.split("/")
        avatar.src = base + "static/" + endSplit[1] + "/plugin/gamekins/avatars/" + split[split.length - 1]
    }
});