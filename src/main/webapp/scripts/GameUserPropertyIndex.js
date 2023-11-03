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

                    if (list.length === 0) {
                        div.style.display = "none"
                    } else {
                        for(let i = 0; i < list.length; i++) {
                            createAchievement(div, list[i], false)
                            div.appendChild(document.createElement("br"))
                        }
                    }

                    if (currentUser) {
                        let header = document.getElementById("completedAchievementsHeader")
                        header.innerText = "Other Completed Achievements" + " (" + list.length + "/" + totalCount + ")"
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

            new Ajax.Request(window.location.href + "getProgressAchievements", {
                parameters: parameters,
                onComplete: function (rsp) {

                    let list = JSON.parse(rsp.responseText)

                    for(let i = 0; i < list.length; i++) {
                        let progressAchievements = document.getElementById("progressAchievements")
                        createProgressAchievement(progressAchievements, list[i], currentUser)
                        progressAchievements.appendChild(document.createElement("br"))
                    }

                }
            })

            new Ajax.Request(window.location.href + "getBadgeAchievements", {
                parameters: parameters,
                onComplete: function (rsp) {

                    let list = JSON.parse(rsp.responseText)

                    for(let i = 0; i < list.length; i++) {
                        let badgeAchievements = document.getElementById("badgeAchievements")
                        createBadgeAchievement(badgeAchievements, list[i])
                        badgeAchievements.appendChild(document.createElement("br"))
                    }

                    jQuery3("img").click(function () {
                                var inputElement = jQuery3(this).attr('id')
                                unclickBadge(inputElement)
                                clickBadge(inputElement)
                            });
                }
            })

        }
    })

    function createBadgeAchievement(parent, achievement) {

        let table = document.createElement("table")
        table.style.border = "2px solid grey"
        table.style.width = "50%"
        table.style.marginLeft = "auto"
        table.style.marginRight = "auto"
        table.style.tableLayout = "fixed"

        let tr1 = document.createElement("tr")
        let td1 = document.createElement("td")

        td1.colSpan = achievement.badgePaths.length
        td1.style.verticalAlign = "bottom"
        td1.style.textAlign = "left"
        let b = document.createElement("b")
        b.innerText = achievement.title
        td1.appendChild(b)
        tr1.appendChild(td1)
        table.appendChild(tr1)

        let tr2 = document.createElement("tr")
        let td2 = document.createElement("td")

        td2.colSpan = achievement.badgePaths.length
        td2.style.verticalAlign = "bottom"
        td2.style.textAlign = "left"

        td2.innerText = achievement.description

        tr2.appendChild(td2)
        table.appendChild(tr2)

        let badgeRow = document.createElement("tr")
        let badgeCell
        let img

        for (let i = 0; i < achievement.badgePaths.length; i++) {
            badgeCell = document.createElement("td")
            img = document.createElement("img")
            let src = document.getElementsByTagName("img")[0].src
            let base = src.substring(0, src.indexOf("static"))
            let endSplit = src.substring(src.indexOf("static")).split("/")
            if (achievement.badgeCounts[i] === 0) {
                img.src = base + "static/" + endSplit[1] + achievement.badgePaths[i].replace("colour", "blackwhite").replace("-colour.png", ".png")
            }
            else {
                img.src = base + "static/" + endSplit[1] + achievement.badgePaths[i]
            }
            img.style.maxWidth = "100%"
            img.style.maxHeight = "100%"

            img.style.cursor = "pointer"

            img.id = achievement.title.replaceAll(" ", "") + i
            img.setAttribute('data-achievementName', achievement.title.replace(" ", ""))
            img.setAttribute('data-title', achievement.titles[i])
            img.setAttribute('data-lowerBound', achievement.lowerBounds[i])
            img.setAttribute('data-amount', achievement.badgeCounts[i])
            if (i + 1 < achievement.lowerBounds.length) {
                img.setAttribute('data-upperBound', achievement.lowerBounds[i + 1])
            } else {
                img.setAttribute('data-upperBound', "---")
            }
            img.setAttribute('data-unit', achievement.unit)

            badgeCell.appendChild(img)

            badgeRow.appendChild(badgeCell)
        }
        table.appendChild(badgeRow)

        let tr3 = document.createElement("tr")
        let td3 = document.createElement("td")

        let div = document.createElement("div")
        div.id = achievement.title.replace(" ", "")

        td3.colSpan = achievement.badgePaths.length

        td3.appendChild(div)

        tr3.appendChild(td3)
        table.appendChild(tr3)

        parent.appendChild(table)



    }

    function createProgressAchievement(parent, achievement, displayBars) {
        let milestones = achievement.milestones

        let table = document.createElement("table")
        table.style.border = "2px solid grey"
        table.style.width = "50%"
        table.style.marginLeft = "auto"
        table.style.marginRight = "auto"
        table.style.tableLayout = "fixed"

        let tr1 = document.createElement("tr")

        let td1 = document.createElement("td")
        td1.rowSpan = 3
        td1.style.paddingRight = "5px"
        td1.style.verticalAlign = "middle"
        td1.style.width = "50px"
        let div = document.createElement("div")
        div.style.position = "relative"
        div.style.verticalAlign = "middle"
        let img = document.createElement("img")
        let src = document.getElementsByTagName("img")[0].src
        let base = src.substring(0, src.indexOf("static"))
        let endSplit = src.substring(src.indexOf("static")).split("/")
        if (achievement.progress <= 0) {
            img.src = base + "static/" + endSplit[1] + achievement.badgePath.replace("-colour.png", ".png").replace("colour", "blackwhite")
        } else {
            img.src = base + "static/" + endSplit[1] + achievement.badgePath
            img.style.maxWidth = "100%"
            img.style.maxHeight = "100%"
            img.style.marginTop = "15px"
            let active = -1;
            for (let i = 0; i < milestones.length; i++) {
                if (achievement.progress >= milestones[i]) {
                    active = i
                }
            }
            let starPath;
            if (active >= 3) {
                starPath = base + "static/" + endSplit[1] + achievement.badgePath
                    .substring(0, achievement.badgePath.lastIndexOf("/")).replace("colour", "stars") + "/StarGold.png"
                active -= 3
            } else {
                starPath = base + "static/" + endSplit[1] + achievement.badgePath
                    .substring(0, achievement.badgePath.lastIndexOf("/")).replace("colour", "stars") + "/StarSilver.png"
            }

            let starMiddle = document.createElement("img")
            starMiddle.src = starPath
            starMiddle.style.position = "absolute"
            starMiddle.style.left = "50%"
            starMiddle.style.marginLeft = "-6px";
            starMiddle.height = 12
            starMiddle.width = 12

            let starLeft = document.createElement("img")
            starLeft.src = starPath
            starLeft.style.position = "absolute"
            starLeft.style.left = "30%"
            starLeft.style.marginLeft = "-6px";
            starLeft.height = 12
            starLeft.width = 12

            let starRight = document.createElement("img")
            starRight.src = starPath
            starRight.style.position = "absolute"
            starRight.style.left = "70%"
            starRight.style.marginLeft = "-6px";
            starRight.height = 12
            starRight.width = 12
            switch (active) {
                case 0:
                    div.appendChild(starMiddle)
                    break
                case 1:
                    div.appendChild(starLeft)
                    div.appendChild(starRight)
                    break
                case 2:
                    div.appendChild(starMiddle)
                    starMiddle.style.marginTop = "-6px"
                    starLeft.style.left = "20%"
                    starRight.style.left = "80%"
                    div.appendChild(starLeft)
                    div.appendChild(starRight)
                    break
                default:
                    break
            }
        }

        img.height = 48
        img.width = 48
        div.appendChild(img)
        td1.appendChild(div)
        tr1.appendChild(td1)

        let td2 = document.createElement("td")
        td2.style.verticalAlign = "bottom"
        td2.style.textAlign = "left"
        td2.colSpan = achievement.milestones.length
        let b = document.createElement("b")
        b.innerText = achievement.title
        td2.appendChild(b)
        tr1.appendChild(td2)

        let tr2 = document.createElement("tr")

        if (displayBars) {
            for(let i = 0; i < milestones.length; i++) {
                let cell = document.createElement("td")
                let progContainer = document.createElement("div")
                progContainer.classList.add("progress")
                let progBar = document.createElement("div")
                progBar.classList.add("progress-bar")
                progBar.classList.add("overflow-visible")
                progBar.classList.add("text-dark")
                progBar.classList.add("bg-info")
                progBar.setAttribute("role", "progressbar")
                if (i === 0) {
                    progBar.setAttribute("aria-valuemin", "0")
                    progBar.style.width = (achievement.progress / milestones[i]) * 100 + "%"
                    if (achievement.progress <= milestones[i] && achievement.progress > 0) {
                        progBar.innerText = achievement.progress + achievement.unit
                    }
                }
                else {
                    progBar.setAttribute("aria-valuemin", milestones[i - 1].toString())
                    progBar.style.width = ((achievement.progress - milestones[i - 1]) / (milestones[i] - milestones[i - 1])) * 100 + "%"
                    if ((achievement.progress <= milestones[i] || i == milestones.length-1) && achievement.progress > milestones[i - 1]) {
                        progBar.innerText = achievement.progress + achievement.unit
                    }
                }
                progBar.setAttribute("aria-valuenow", achievement.progress.toString())
                progBar.setAttribute("aria-valuemax", milestones[i])


                progContainer.appendChild(progBar)
                cell.appendChild(progContainer)
                cell.append(milestones[i] + achievement.unit)
                cell.style.textAlign = "right"
                tr2.appendChild(cell)
            }
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
function unclickBadge(inputElement) {
    let badge = document.getElementById(inputElement)
    let badges = badge.parentElement.parentElement.getElementsByTagName("img")
    for (let i = 0; i < badges.length; i++) {
        if (badges[i].id != inputElement) {
            badges[i].style.boxShadow = ""
        }
    }
}

function clickBadge(inputElement) {
    let badge = document.getElementById(inputElement)

    let div = document.getElementById(badge.getAttribute("data-achievementName"))
    while (div.firstChild) {
        div.removeChild(div.lastChild);
    }

    if (badge.style.boxShadow == "") {
        badge.style.boxShadow = "0 0 1px 1px #292b2c"

        let title = document.createElement("b")
        title.innerText = badge.getAttribute("data-title")
        div.appendChild(title)
        let lowerBound = document.createElement("div")
        lowerBound.innerText = "Lower boundary: " + badge.getAttribute("data-lowerBound") + badge.getAttribute("data-unit")
        div.appendChild(lowerBound)

        if (badge.getAttribute("data-upperBound") !== "---") {
            let upperBound = document.createElement("div")
            upperBound.innerText += "Upper boundary: " + badge.getAttribute("data-upperBound") + badge.getAttribute("data-unit")
            div.appendChild(upperBound)
        }

        let amount = document.createElement("div")
        amount.innerText += "Amount: " + jQuery3("#" + inputElement).data("amount")
        div.appendChild(amount)
    } else {
        badge.style.boxShadow = ""
    }
}