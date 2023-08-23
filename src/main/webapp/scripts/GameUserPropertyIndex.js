let projects = document.getElementById("projects")
let urlCurrentUser = window.location.href + "isCurrentUser"
let currentUser = false

new Ajax.Request(urlCurrentUser, {
    onComplete: function (rsp) {
        currentUser = (rsp.responseText === "true")
    }
})

window.onload = function () {
    let url = window.location.href + "getProjects"

    new Ajax.Request(url, {
        onComplete: function (rsp) {
            let obj = JSON.parse(rsp.responseText)
            for(let i = 0; i < obj.length; i++) {
                let option = document.createElement("option")
                option.text = obj[i]
                projects.add(option)
            }

            if (currentUser) changeProjectURL()

            changeLeaderboardURL()

            loadAchievements()
        }
    })
}

projects.addEventListener("change", function () {
    changeProjectURL()
    loadAchievements()
})

function changeLeaderboardURL() {
    let url = window.location.href + "getLastLeaderboard"
    new Ajax.Request(url, {
        onComplete: function (rsp) {
            let link = document.getElementById("leaderboardLink")

            link.href = rsp.responseText
            link.innerText = "Go to last leaderboard"
        }
    })
}

function changeProjectURL() {
    let link = document.getElementById("projectLink")
    let baseURL = window.location.origin + window.location.href.substring(window.location.origin.length, window.location.href.indexOf("user"))
    let projectName = projects.value.replaceAll("/", "/job/")
    link.href = baseURL + "job/" + projectName + "/leaderboard/"
    link.innerText = "Go to project leaderboard"
}

function loadAchievements() {
    let urlUnsolved = window.location.href + "getUnsolvedAchievements"
    let urlCompleted = window.location.href + "getCompletedAchievements"
    let urlTotalCount = window.location.href + "getAchievementsCount"
    let urlSecretCount = window.location.href + "getUnsolvedSecretAchievementsCount"
    let parameters = {}
    parameters["projectName"] = projects.value

    new Ajax.Request(urlTotalCount, {
        parameters: parameters,
        onComplete: function (rsp) {
            let totalCount = rsp.responseText

            new Ajax.Request(urlCompleted, {
                parameters: parameters,
                onComplete: function (rsp) {
                    let list = JSON.parse(rsp.responseText)
                    let div = document.getElementById("completedAchievements")
                    while (div.childNodes.length > 1) {
                        div.removeChild(div.lastChild)
                    }

                    if (list.length === 0 && currentUser) {
                        let header = document.createElement("h5")
                        header.style.display = "block"
                        header.style.textAlign = "center"
                        header.innerText = "You have not solved any achievements yet"
                        div.appendChild(header)
                    } else {
                        for(let i = 0; i < list.length; i++) {
                            createAchievement(div, list[i], false)
                            div.appendChild(document.createElement("br"))
                        }
                    }

                    if (currentUser) {
                        let header = document.getElementById("completedAchievementsHeader")
                        header.innerText = "Completed Achievements" + " (" + list.length + "/" + totalCount + ")"
                    }
                }
            })

            if (currentUser) {
                new Ajax.Request(urlUnsolved, {
                    parameters: parameters,
                    onComplete: function (rsp) {
                        let list = JSON.parse(rsp.responseText)
                        let div = document.getElementById("unsolvedAchievements")
                        while (div.childNodes.length > 1) {
                            div.removeChild(div.lastChild)
                        }

                        for(let i = 0; i < list.length; i++) {
                            createAchievement(div, list[i], true)
                            div.appendChild(document.createElement("br"))
                        }

                        let header = document.getElementById("unsolvedAchievementsHeader")
                        header.innerText = "Unsolved Achievements" + " (" + list.length + ")"
                    }
                })

                new Ajax.Request(urlSecretCount, {
                    parameters: parameters,
                    onComplete: function (rsp) {
                        let header = document.getElementById("secretAchievementsHeader")
                        header.innerText = "Secret Achievements" + " (" + rsp.responseText + ")"
                    }
                })
            } else {
                let div = document.getElementById("unsolvedAchievements")
                div.style.display = "none"
                div = document.getElementById("secretAchievements")
                div.style.display = "none"
            }

            new Ajax.Request(window.location.href + "getProgressAchievementExample", {
                parameters: parameters,
                onComplete: function (rsp) {
                    console.log(rsp.responseText)

                    let list = JSON.parse(rsp.responseText)

                    for(let i = 0; i < list.length; i++) {
                        let progressAchievements = document.getElementById("newAchievements")
                        createProgressAchievement(progressAchievements, list[i])
                    }

                }
            })
        }
    })

    function createProgressAchievement(parent, achievement) {
        let milestones = achievement.milestones

        let table = document.createElement("table")
        table.style.border = "2px solid grey"
        table.style.width = "50%"
        table.style.marginLeft = "auto"
        table.style.marginRight = "auto"
        table.style.tableLayout = "fixed"

        let tr1 = document.createElement("tr")

        let td1 = document.createElement("td")
        td1.rowSpan = 2
        td1.style.paddingRight = "5px"
        td1.style.verticalAlign = "middle"
        td1.style.width = "50px"
        let img = document.createElement("img")
        let src = document.getElementsByTagName("img")[0].src
        let base = src.substring(0, src.indexOf("static"))
        let endSplit = src.substring(src.indexOf("static")).split("/")
        if (achievement.progress <= 0) {
            img.src = base + "static/" + endSplit[1] + achievement.badgeBasePath
        } else {
            img.src = base + "static/" + endSplit[1] + achievement.badgeBasePath.replace("blackwhite", "colour").replace(".png", "-colour.png")
            let active = -1;
            for (let i = 0; i < milestones.length; i++) {
                if (achievement.progress > milestones[i]) {
                    active = i
                }
            }
            switch (active) {
                case 0:
                    let star = document.createElement("img")
                    star.src = base + "static/" + endSplit[1] + achievement.badgeBasePath
                        .substring(0, achievement.badgeBasePath.lastIndexOf("/")).replace("blackwhite", "stars") + "/004-badge.png"
                    star.style.position = "absolute"
                    star.height = 12
                    star.width = 12
                    td1.appendChild(star)
                    break
                default:
                    break
            }
        }

        img.height = 48
        img.width = 48
        td1.appendChild(img)
        tr1.appendChild(td1)

        let td2 = document.createElement("td")
        td2.style.verticalAlign = "bottom"
        td2.style.textAlign = "left"
        let b = document.createElement("b")
        b.innerText = achievement.title
        td2.appendChild(b)
        tr1.appendChild(td2)

        let tr2 = document.createElement("tr")


        for(let i = 0; i < milestones.length; i++) {
            let cell = document.createElement("td")
            let progContainer = document.createElement("div")
            progContainer.classList.add("progress")
            let progBar = document.createElement("div")
            progBar.classList.add("progress-bar")
            progBar.setAttribute("role", "progressbar")
            if (i === 0) {
                progBar.setAttribute("aria-valuemin", "0")
                progBar.style.width = (achievement.progress / milestones[i]) * 100 + "%"
                if (achievement.progress <= milestones[i] && achievement.progress > 0) {
                    progBar.innerText = achievement.progress
                }
            }
            else {
                progBar.setAttribute("aria-valuemin", milestones[i - 1].toString())
                progBar.style.width = ((achievement.progress - milestones[i - 1]) / (milestones[i] - milestones[i - 1])) * 100 + "%"
                if (achievement.progress <= milestones[i] && achievement.progress > milestones[i - 1]) {
                    progBar.innerText = achievement.progress
                }
            }
            progBar.setAttribute("aria-valuenow", achievement.progress.toString())
            progBar.setAttribute("aria-valuemax", milestones[i])


            progContainer.appendChild(progBar)
            cell.appendChild(progContainer)
            cell.append(milestones[i])
            cell.style.textAlign = "right"
            tr2.appendChild(cell)
        }

        let tr3 = document.createElement("tr")
        let td3 = document.createElement("td")
        td3.colSpan = milestones.length
        td3.innerText = achievement.description

        tr3.appendChild(td3)

        table.appendChild(tr1)
        table.appendChild(tr2)
        table.appendChild(tr3)
        parent.appendChild(table)
    }

    function createAchievement(parent, achievement, unsolved) {
        let table = document.createElement("table")
        table.style.border = "2px solid grey"
        table.style.width = "50%"
        table.style.marginLeft = "auto"
        table.style.marginRight = "auto"

        let tr1 = document.createElement("tr")

        let td1 = document.createElement("td")
        td1.rowSpan = 2
        td1.style.paddingRight = "5px"
        td1.style.verticalAlign = "middle"
        td1.style.width = "50px"
        let img = document.createElement("img")
        let src = document.getElementsByTagName("img")[0].src
        let base = src.substring(0, src.indexOf("static"))
        let endSplit = src.substring(src.indexOf("static")).split("/")
        if (unsolved) {
            img.src = base + "static/" + endSplit[1] + achievement.unsolvedBadgePath
        } else {
            img.src = base + "static/" + endSplit[1] + achievement.badgePath
        }

        img.height = 48
        img.width = 48
        td1.appendChild(img)
        tr1.appendChild(td1)

        let td2 = document.createElement("td")
        td2.style.verticalAlign = "bottom"
        td2.style.textAlign = "left"
        let b = document.createElement("b")
        b.innerText = achievement.title
        td2.appendChild(b)
        tr1.appendChild(td2)

        let td3 = document.createElement("td")
        td3.rowSpan = 2
        td3.style.verticalAlign = "middle"
        td3.style.textAlign = "right"
        td3.style.width = "33%"
        td3.innerText = achievement.solvedTimeString
        tr1.appendChild(td3)

        let tr2 = document.createElement("tr")

        let td4 = document.createElement("td")
        td4.style.verticalAlign = "top"
        td4.style.textAlign = "left"
        let i = document.createElement("i")
        i.innerText = achievement.description
        td4.appendChild(i)
        tr2.appendChild(td4)

        table.appendChild(tr1)
        table.appendChild(tr2)
        parent.appendChild(table)
    }
}