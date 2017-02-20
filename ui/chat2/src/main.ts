/// <reference path="../dts/index.d.ts" />

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'

import makeCtrl from './ctrl';
import view from './view';
import { ChatOpts, Ctrl } from './interfaces'

const snabbdomModules = [
  require('snabbdom/modules/class').default,
  require('snabbdom/modules/props').default,
  require('snabbdom/modules/attributes').default,
  require('snabbdom/modules/eventlisteners').default
]

let patch = init(snabbdomModules);

export default function LichessChat(element: Element, opts: ChatOpts) {

  let vnode: VNode, ctrl: Ctrl

  let redraw = () => {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw)

  patch(element, view(ctrl))

  window.lichess.pubsub.emit('chat.ready', ctrl)

  window.Mousetrap.bind('/', function() {
    (element.querySelector('input.lichess_say') as HTMLElement).focus();
    return false;
  })

  return {
    preset: ctrl.preset
  };
};