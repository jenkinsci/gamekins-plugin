jQuery3("#addTeam").on('click', function () {
    let teamName = jQuery3("#teamName")[0].value
    let descriptorFullUrl = jQuery3(this).data('descriptor-url')
    let url = descriptorFullUrl + "/addTeam"
    let parameters = {}
    parameters["teamName"] = teamName

    new Ajax.Request(url, {
        parameters: parameters,
        onComplete: function (rsp) {
            if (!rsp.responseText.includes("class=error")) {
                jQuery3("#teamName")[0].value = ""
                buildTeamsList(descriptorFullUrl)
            }
            jQuery3("#error-user-team")[0].innerHTML = rsp.responseText
        }
    })
})

jQuery3("#addUserToTeam").on('click', function () {
    let teamsBox = jQuery3("#teamsBox")[0].value
    let usersBox = jQuery3("#usersBox")[0].value
    let descriptorFullUrl = jQuery3(this).data('descriptor-url')
    let url = descriptorFullUrl + "/addUserToTeam"
    let parameters = {}
    parameters["teamsBox"] = teamsBox
    parameters["usersBox"] = usersBox

    new Ajax.Request(url, {
        parameters: parameters,
        onComplete: function (rsp) {
            jQuery3("#error-user-team")[0].innerHTML = rsp.responseText
        }
    })
})

jQuery3("#removeUserFromTeam").on('click', function () {
    let teamsBox = jQuery3("#teamsBox")[0].value
    let usersBox = jQuery3("#usersBox")[0].value
    let descriptorFullUrl = jQuery3(this).data('descriptor-url')
    let url = descriptorFullUrl + "/removeUserFromTeam"
    let parameters = {}
    parameters["teamsBox"] = teamsBox
    parameters["usersBox"] = usersBox

    new Ajax.Request(url, {
        parameters: parameters,
        onComplete: function (rsp) {
            jQuery3("#error-user-team")[0].innerHTML = rsp.responseText
        }
    })
})

jQuery3("#deleteTeam").on('click', function () {
    let teamsBox = jQuery3("#teamsBox")[0].value
    let descriptorFullUrl = jQuery3(this).data('descriptor-url')
    let url = descriptorFullUrl + "/deleteTeam"
    let parameters = {}
    parameters["teamsBox"] = teamsBox

    new Ajax.Request(url, {
        parameters: parameters,
        onComplete: function (rsp) {
            jQuery3("#error-user-team")[0].innerHTML = rsp.responseText

            buildTeamsList(descriptorFullUrl)
        }
    })
})

jQuery3("#showTeamMemberships").on('click', function () {
    let descriptorFullUrl = jQuery3(this).data('descriptor-url')
    buildTeamsTable(descriptorFullUrl)
})

function buildTeamsList(descriptorFullUrl) {
    let url = descriptorFullUrl + "/fillTeamsBoxItems"

    new Ajax.Request(url, {
        onComplete: function (rsp) {
            let teamsBox = jQuery3("#teamsBox")[0]
            let values = rsp.responseJSON.values

            teamsBox.innerHTML = ""
            for (let i = 0; i < values.length; i++) {
                const opt = values[i].name;
                const el = document.createElement("option");
                el.textContent = opt;
                el.value = opt;
                teamsBox.appendChild(el);
            }

            let table = jQuery3("#team-table")[0]
            if (!table.innerHTML.empty()) {
                buildTeamsTable(descriptorFullUrl, true)
            }
        }
    })
}

function buildTeamsTable(descriptorFullUrl, rebuild = false) {
    let url = descriptorFullUrl + "/showTeamMemberships"

    new Ajax.Request(url, {
        onComplete: function (rsp) {
            url = descriptorFullUrl + "/fillTeamsBoxItems"
            let teamMap = JSON.parse(rsp.responseText)

            new Ajax.Request(url, {
                onComplete: function (rsp) {
                    let teams = rsp.responseJSON.values

                    let table = jQuery3("#team-table")[0]

                    if (table.innerHTML !== "") {
                        table.innerHTML = ""
                        if (!rebuild) return
                    }

                    let header = document.createElement("thead")
                    let headTr = document.createElement("tr")
                    let th1 = document.createElement("th")
                    let th2 = document.createElement("th")

                    th1.setAttribute("scope", "col")
                    th1.innerText = "Team"
                    headTr.appendChild(th1)

                    th2.setAttribute("scope", "col")
                    th2.innerText = "User"
                    headTr.appendChild(th2)

                    header.appendChild(headTr)
                    table.appendChild(header)

                    let body = document.createElement("tbody")

                    for(let i = 0; i < teams.length; i++) {
                        let team = teams[i].name
                        let users = teamMap[team]

                        let tr = document.createElement("tr")
                        let th = document.createElement("th")
                        let td = document.createElement("td")

                        th.innerText = team
                        if (users.length > 0) {
                            th.setAttribute("rowspan", users.length)
                            td.innerText = users[0]
                        } else {
                            th.setAttribute("rowspan", "1")
                        }

                        tr.appendChild(th)
                        tr.appendChild(td)
                        body.appendChild(tr)

                        for(let j = 1; j < users.length; j++) {
                            let innerTr = document.createElement("tr")
                            let innerTd = document.createElement("td")

                            innerTd.innerText = users[j]

                            innerTr.appendChild(innerTd)
                            body.appendChild(innerTr)
                        }
                    }

                    table.appendChild(body)
                }
            })
        }
    })
}